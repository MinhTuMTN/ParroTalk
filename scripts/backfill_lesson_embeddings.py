#!/usr/bin/env python3
"""Backfill and smoke-test lesson embeddings via Cloudflare Workers AI (@cf/baai/bge-m3)."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import re
import sys
import time
from pathlib import Path
from typing import Any, Callable, Mapping, Sequence
from urllib.parse import unquote, urlparse

import requests

DEFAULT_MODEL = "@cf/baai/bge-m3"
EXPECTED_DIMS = 1024
MAX_SEARCH_CHARS = 12000
DEFAULT_BATCH_SIZE = 8
DEFAULT_DELAY_MS = 150
DEFAULT_MAX_RETRIES = 4
MAX_RETRY_AFTER_SECONDS = 120
EXIT_RATE_LIMITED = 2

ROOT_DIR = Path(__file__).resolve().parent.parent


class RateLimitExhaustedError(RuntimeError):
    """Raised when Cloudflare Workers AI rate limit or daily quota is exhausted."""


def _clean_text(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", value).strip()


def format_categories(categories: Sequence[tuple[str | None, str | None]]) -> str:
    """Format deduplicated, sorted category labels with optional Vietnamese name."""
    deduped: dict[str, tuple[str, str]] = {}
    for raw_name, raw_name_vi in categories:
        name = _clean_text(raw_name)
        if not name:
            continue
        name_vi = _clean_text(raw_name_vi)
        key = name.lower()
        existing = deduped.get(key)
        if existing is None or (not existing[1] and name_vi):
            deduped[key] = (name, name_vi)

    formatted: list[str] = []
    for _, (name, name_vi) in sorted(deduped.items(), key=lambda item: item[0]):
        if name_vi and name_vi.lower() != name.lower():
            formatted.append(f"{name} ({name_vi})")
        else:
            formatted.append(name)
    return ", ".join(formatted)


def _truncate_at_word_boundary(text: str, budget: int) -> str:
    if budget <= 0 or not text:
        return ""
    if len(text) <= budget:
        return text
    sliced = text[:budget]
    if not text[budget].isspace():
        last_space = sliced.rfind(" ")
        if last_space > 0:
            sliced = sliced[:last_space]
    return sliced.rstrip()


def build_search_content(
    title: str | None,
    program: str | None,
    categories: Sequence[tuple[str | None, str | None]],
    description: str | None,
    transcript: str | None,
    max_chars: int = MAX_SEARCH_CHARS,
) -> str:
    """Build structured bilingual search text, preserving metadata and capping length."""
    metadata_lines: list[str] = []
    clean_title = _clean_text(title)
    if clean_title:
        metadata_lines.append(f"Title: {clean_title}")

    clean_program = _clean_text(program)
    if clean_program:
        metadata_lines.append(f"Program: {clean_program}")

    cat_str = format_categories(categories)
    if cat_str:
        metadata_lines.append(f"Categories: {cat_str}")

    clean_desc = _clean_text(description)
    if clean_desc:
        metadata_lines.append(f"Description: {clean_desc}")

    clean_transcript = _clean_text(transcript)
    metadata_block = "\n".join(metadata_lines)

    if not clean_transcript:
        return metadata_block[:max_chars]

    prefix = ("\n" if metadata_block else "") + "Transcript: "
    transcript_budget = max_chars - len(metadata_block) - len(prefix)
    if transcript_budget <= 0:
        return metadata_block[:max_chars]

    truncated_transcript = _truncate_at_word_boundary(clean_transcript, transcript_budget)
    if not truncated_transcript:
        return metadata_block[:max_chars]

    return f"{metadata_block}{prefix}{truncated_transcript}"


def compute_content_hash(model: str, search_content: str) -> str:
    """Return deterministic SHA-256 hex digest of model identifier and search content."""
    payload = f"{model}\n{search_content}".encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def validate_embeddings(
    vectors: Any,
    expected_count: int,
    expected_dims: int = EXPECTED_DIMS,
) -> list[list[float]]:
    """Validate embedding count, dimensionality, and finite numeric values."""
    if not isinstance(vectors, list) or len(vectors) != expected_count:
        got = len(vectors) if isinstance(vectors, list) else type(vectors).__name__
        raise ValueError(f"Expected {expected_count} embedding vectors, got {got}")

    validated: list[list[float]] = []
    for idx, vec in enumerate(vectors):
        if not isinstance(vec, list) or len(vec) != expected_dims:
            got_dim = len(vec) if isinstance(vec, list) else type(vec).__name__
            raise ValueError(
                f"Vector at index {idx} has invalid dimension: expected {expected_dims}, got {got_dim}"
            )
        cleaned_vec: list[float] = []
        for elem_idx, val in enumerate(vec):
            if isinstance(val, bool) or not isinstance(val, (int, float)) or not math.isfinite(val):
                raise ValueError(
                    f"Vector at index {idx} element {elem_idx} is invalid: {val!r}"
                )
            cleaned_vec.append(float(val))
        validated.append(cleaned_vec)
    return validated


def vector_to_pg_literal(vec: Sequence[float]) -> str:
    """Serialize float vector to pgvector text literal format."""
    return json.dumps(list(vec), separators=(",", ":"))


def parse_db_connect_kwargs(
    database_url: str | None = None,
    env: Mapping[str, str] | None = None,
) -> dict[str, Any]:
    """Parse PostgreSQL connection kwargs with strict CLI > DB_URL > DATABASE_URL > PG* precedence."""
    source_env = os.environ if env is None else env
    raw_url = (
        (database_url or "").strip()
        or (source_env.get("DB_URL") or "").strip()
        or (source_env.get("DATABASE_URL") or "").strip()
    )

    if raw_url:
        if raw_url.startswith("jdbc:"):
            raw_url = raw_url[len("jdbc:") :]
        parsed = urlparse(raw_url)
        if parsed.scheme.startswith("postgres"):
            path_db = (parsed.path or "").lstrip("/")
            user = unquote(parsed.username) if parsed.username else (
                source_env.get("DB_USERNAME") or source_env.get("PGUSER") or "admin"
            )
            password = unquote(parsed.password) if parsed.password is not None else (
                source_env.get("DB_PASSWORD") or source_env.get("PGPASSWORD") or ""
            )
            return {
                "host": parsed.hostname or source_env.get("PGHOST") or source_env.get("DB_HOST") or "localhost",
                "port": parsed.port or int(source_env.get("PGPORT") or source_env.get("DB_PORT") or "5432"),
                "dbname": path_db or source_env.get("PGDATABASE") or source_env.get("DB_NAME") or "parrotalk",
                "user": user,
                "password": password,
            }
        raise ValueError(f"Unsupported database URL scheme: {parsed.scheme or 'unknown'}")

    return {
        "host": source_env.get("PGHOST") or source_env.get("DB_HOST") or "localhost",
        "port": int(source_env.get("PGPORT") or source_env.get("DB_PORT") or "5432"),
        "dbname": source_env.get("PGDATABASE") or source_env.get("DB_NAME") or "parrotalk",
        "user": source_env.get("DB_USERNAME") or source_env.get("PGUSER") or "admin",
        "password": source_env.get("DB_PASSWORD") or source_env.get("PGPASSWORD") or "",
    }


def load_env_files(
    env_file: str | None = None,
    default_env_paths: Sequence[Path | str] | None = None,
) -> None:
    """Load repo .env files without overriding shell vars, plus Cloudflare keys only from --env-file."""
    from dotenv import dotenv_values, load_dotenv

    paths = (
        list(default_env_paths)
        if default_env_paths is not None
        else [
            Path(__file__).resolve().parent / ".env",
            ROOT_DIR / "parro-talk-audio-service" / ".env",
            ROOT_DIR / ".env",
        ]
    )
    for path in paths:
        path_obj = Path(path)
        if path_obj.is_file():
            load_dotenv(path_obj, override=False)

    if env_file:
        env_path = Path(env_file)
        if not env_path.is_file():
            raise FileNotFoundError(f"Env file not found: {env_file}")
        values = dotenv_values(env_path)
        for key in ("CLOUDFLARE_ACCOUNT_ID", "CLOUDFLARE_API_TOKEN"):
            val = values.get(key)
            if val and not os.environ.get(key):
                os.environ[key] = val


def connect_db(database_url: str | None = None):
    """Connect to PostgreSQL using psycopg2 and print sanitized target info."""
    import psycopg2

    kwargs = parse_db_connect_kwargs(database_url=database_url)
    print(f"Connected to DB: {kwargs['host']}:{kwargs['port']}/{kwargs['dbname']}")
    return psycopg2.connect(**kwargs)


def _parse_retry_after(headers: Mapping[str, Any] | None) -> float | None:
    if not headers:
        return None
    for key, value in headers.items():
        if key.lower() == "retry-after":
            try:
                parsed = float(str(value).strip())
                return parsed if parsed >= 0 else None
            except ValueError:
                return None
    return None


def _summarize_cloudflare_error(resp: Any, payload: dict[str, Any] | None) -> tuple[str, bool]:
    """Return sanitized error summary and whether payload indicates rate-limit/quota (code 3036)."""
    is_quota = False
    if isinstance(payload, dict):
        errors = payload.get("errors") or []
        parts: list[str] = []
        for err in errors:
            if isinstance(err, dict):
                code = err.get("code")
                msg = str(err.get("message") or "")
                if str(code) == "3036":
                    is_quota = True
                parts.append(f"{code}: {msg}" if code is not None else msg)
            else:
                parts.append(str(err))
        if parts:
            return "; ".join(parts)[:300], is_quota
    text = str(getattr(resp, "text", "") or "").strip()
    return text[:300] or "Unknown Cloudflare API error", is_quota


def embed_texts(
    texts: list[str],
    account_id: str,
    api_token: str,
    model: str = DEFAULT_MODEL,
    timeout: int = 120,
    max_retries: int = DEFAULT_MAX_RETRIES,
    sleep_fn: Callable[[float], None] = time.sleep,
    post_fn: Callable[..., Any] = requests.post,
) -> list[list[float]]:
    """Embed a batch of texts using Cloudflare Workers AI REST API with retry and 429 handling."""
    if not texts:
        return []
    if not account_id or not api_token:
        raise ValueError("Missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN")

    url = f"https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/run/{model}"
    headers = {
        "Authorization": f"Bearer {api_token}",
        "Content-Type": "application/json",
    }
    body = {"text": texts, "truncate_inputs": True}

    for attempt in range(max_retries + 1):
        try:
            resp = post_fn(url, headers=headers, json=body, timeout=timeout)
        except requests.RequestException as exc:
            if attempt >= max_retries:
                raise RuntimeError(
                    f"Cloudflare API request failed after {max_retries + 1} attempts: {exc}"
                ) from exc
            sleep_fn(float(min(2 ** (attempt + 1), 30)))
            continue

        payload: dict[str, Any] | None
        try:
            parsed = resp.json()
            payload = parsed if isinstance(parsed, dict) else None
        except ValueError:
            payload = None

        err_summary, is_quota = _summarize_cloudflare_error(resp, payload)

        if resp.status_code == 429 or (resp.status_code >= 400 and is_quota):
            retry_after = _parse_retry_after(getattr(resp, "headers", None))
            if retry_after is not None and retry_after > MAX_RETRY_AFTER_SECONDS:
                raise RateLimitExhaustedError(
                    f"Cloudflare rate limit / daily quota reached (Retry-After={retry_after}s): {err_summary}"
                )
            if attempt >= max_retries:
                raise RateLimitExhaustedError(
                    f"Cloudflare rate limit exhausted after {max_retries + 1} attempts: {err_summary}"
                )
            delay = retry_after if (retry_after is not None and retry_after > 0) else float(min(2 ** (attempt + 1), 30))
            sleep_fn(delay)
            continue

        if resp.status_code in (500, 502, 503, 504):
            if attempt >= max_retries:
                raise RuntimeError(f"Cloudflare API HTTP {resp.status_code}: {err_summary}")
            sleep_fn(float(min(2 ** (attempt + 1), 30)))
            continue

        if resp.status_code >= 400:
            raise RuntimeError(f"Cloudflare API HTTP {resp.status_code}: {err_summary}")

        if not payload or payload.get("success") is not True:
            if is_quota:
                raise RateLimitExhaustedError(f"Cloudflare quota exhausted: {err_summary}")
            raise RuntimeError(f"Cloudflare API returned failure payload: {err_summary}")

        vectors = (payload.get("result") or {}).get("data")
        return validate_embeddings(vectors, expected_count=len(texts), expected_dims=EXPECTED_DIMS)

    raise RateLimitExhaustedError("Cloudflare rate limit retries exhausted")


def fetch_lessons(conn, include_all: bool = False) -> list[dict[str, Any]]:
    """Fetch lessons with active categories and ordered transcript segments."""
    visibility_filter = "" if include_all else "AND l.status = 'DONE' AND l.visibility_status = 'PUBLISHED'"
    sql = f"""
        SELECT
            l.id::text AS lesson_id,
            l.title,
            l.program,
            l.description,
            COALESCE(
                (
                    SELECT json_agg(json_build_object('name', c.name, 'name_vi', c.name_vi))
                    FROM public.lesson_category_lesson lcl
                    JOIN public.lesson_categories c ON c.id = lcl.category_id
                    WHERE lcl.lesson_id = l.id
                      AND c.is_deleted = false
                      AND c.status = 'ACTIVE'
                ),
                '[]'::json
            ) AS categories_json,
            COALESCE(
                (
                    SELECT string_agg(s.text, ' ' ORDER BY s.display_order ASC, s.start_time ASC)
                    FROM public.transcription_segments s
                    WHERE s.lesson_id = l.id
                      AND s.is_deleted = false
                ),
                ''
            ) AS transcript
        FROM public.lessons l
        WHERE l.is_deleted = false
          {visibility_filter}
        ORDER BY l.id ASC
    """
    lessons: list[dict[str, Any]] = []
    with conn.cursor() as cur:
        cur.execute(sql)
        for lesson_id, title, program, description, categories_json, transcript in cur.fetchall():
            if isinstance(categories_json, str):
                cat_items = json.loads(categories_json)
            else:
                cat_items = categories_json or []
            categories = [
                (item.get("name"), item.get("name_vi"))
                for item in cat_items
                if isinstance(item, dict)
            ]
            lessons.append(
                {
                    "id": str(lesson_id),
                    "title": title,
                    "program": program,
                    "categories": categories,
                    "description": description,
                    "transcript": transcript,
                }
            )
    return lessons


def fetch_existing_hashes(conn) -> dict[str, str]:
    """Fetch existing {lesson_id: content_hash} from public.lesson_embeddings."""
    with conn.cursor() as cur:
        cur.execute("SELECT lesson_id::text, content_hash FROM public.lesson_embeddings")
        return {str(lesson_id): str(content_hash) for lesson_id, content_hash in cur.fetchall()}


def upsert_embeddings_batch(conn, rows: Sequence[dict[str, Any]]) -> None:
    """Upsert one batch of lesson embeddings and commit immediately for crash/429 resumability."""
    if not rows:
        return
    sql = """
        INSERT INTO public.lesson_embeddings (
            lesson_id,
            search_content,
            content_hash,
            embedding,
            embedding_model,
            created_at,
            updated_at
        )
        VALUES (%s::uuid, %s, %s, %s::vector, %s, NOW(), NOW())
        ON CONFLICT (lesson_id) DO UPDATE SET
            search_content = EXCLUDED.search_content,
            content_hash = EXCLUDED.content_hash,
            embedding = EXCLUDED.embedding,
            embedding_model = EXCLUDED.embedding_model,
            updated_at = NOW()
    """
    params = [
        (
            row["lesson_id"],
            row["search_content"],
            row["content_hash"],
            vector_to_pg_literal(row["embedding"]),
            row["embedding_model"],
        )
        for row in rows
    ]
    with conn.cursor() as cur:
        cur.executemany(sql, params)
    conn.commit()


def run_backfill(
    conn,
    embed_fn: Callable[[list[str]], list[list[float]]],
    *,
    model: str = DEFAULT_MODEL,
    batch_size: int = DEFAULT_BATCH_SIZE,
    delay_ms: int = DEFAULT_DELAY_MS,
    force: bool = False,
    dry_run: bool = False,
    limit: int | None = None,
    include_all: bool = False,
    sleep_fn: Callable[[float], None] = time.sleep,
) -> dict[str, int]:
    """Run idempotent, resumable per-batch embedding backfill for lessons."""
    lessons = fetch_lessons(conn, include_all=include_all)
    existing_hashes = {} if force else fetch_existing_hashes(conn)

    pending_items: list[dict[str, str]] = []
    skipped = 0
    for lesson in lessons:
        search_content = build_search_content(
            title=lesson.get("title"),
            program=lesson.get("program"),
            categories=lesson.get("categories") or [],
            description=lesson.get("description"),
            transcript=lesson.get("transcript"),
        )
        if not search_content:
            skipped += 1
            continue
        content_hash = compute_content_hash(model, search_content)
        if not force and existing_hashes.get(lesson["id"]) == content_hash:
            skipped += 1
            continue
        pending_items.append(
            {
                "lesson_id": lesson["id"],
                "search_content": search_content,
                "content_hash": content_hash,
            }
        )

    if limit is not None and limit >= 0:
        pending_items = pending_items[:limit]

    total = len(lessons)
    pending = len(pending_items)
    summary = {
        "total": total,
        "skipped": skipped,
        "pending": pending,
        "upserted": 0,
        "remaining": pending,
        "rate_limited": 0,
        "interrupted": 0,
    }

    if dry_run or pending == 0:
        if dry_run and pending_items:
            max_len = max(len(item["search_content"]) for item in pending_items)
            avg_len = sum(len(item["search_content"]) for item in pending_items) / len(pending_items)
            print(
                f"[DRY RUN] total={total} skipped={skipped} pending={pending} "
                f"avg_chars={avg_len:.1f} max_chars={max_len}"
            )
            print(f"[DRY RUN] Sample[0] ({pending_items[0]['lesson_id']}): {pending_items[0]['search_content'][:200]}...")
        else:
            print(f"Summary: total={total} skipped={skipped} pending={pending} upserted=0")
        return summary

    total_batches = (pending + batch_size - 1) // batch_size
    for batch_idx in range(total_batches):
        start = batch_idx * batch_size
        batch = pending_items[start : start + batch_size]
        texts = [item["search_content"] for item in batch]

        try:
            vectors = embed_fn(texts)
            rows = [
                {
                    "lesson_id": item["lesson_id"],
                    "search_content": item["search_content"],
                    "content_hash": item["content_hash"],
                    "embedding": vec,
                    "embedding_model": model,
                }
                for item, vec in zip(batch, vectors)
            ]
            upsert_embeddings_batch(conn, rows)
            summary["upserted"] += len(rows)
            summary["remaining"] = pending - summary["upserted"]
            print(
                f"Batch {batch_idx + 1}/{total_batches}: upserted={summary['upserted']}/{pending}, "
                f"remaining={summary['remaining']}"
            )
            if delay_ms > 0 and batch_idx + 1 < total_batches:
                sleep_fn(delay_ms / 1000.0)
        except RateLimitExhaustedError as exc:
            conn.rollback()
            summary["rate_limited"] = 1
            print(
                f"Paused due to Cloudflare rate limit/quota: {exc}\n"
                f"Progress saved: Upserted={summary['upserted']}, Skipped={summary['skipped']}, "
                f"Remaining={summary['remaining']}. Re-run the exact same command later to resume.",
                file=sys.stderr,
            )
            break
        except KeyboardInterrupt:
            conn.rollback()
            summary["interrupted"] = 1
            print(
                f"\nInterrupted by user. Progress saved: Upserted={summary['upserted']}, "
                f"Skipped={summary['skipped']}, Remaining={summary['remaining']}. "
                f"Re-run the exact same command later to resume.",
                file=sys.stderr,
            )
            break

    return summary


def run_semantic_search(
    conn,
    query: str,
    embed_fn: Callable[[list[str]], list[list[float]]],
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """Embed query and return top-k semantically similar published lessons."""
    vectors = embed_fn([query])
    vec_literal = vector_to_pg_literal(vectors[0])
    sql = """
        SELECT
            l.id::text,
            l.title,
            l.program,
            ROUND((1 - (le.embedding <=> %s::vector))::numeric, 4) AS similarity
        FROM public.lesson_embeddings le
        JOIN public.lessons l ON l.id = le.lesson_id
        WHERE l.is_deleted = false
          AND l.status = 'DONE'
          AND l.visibility_status = 'PUBLISHED'
        ORDER BY le.embedding <=> %s::vector ASC
        LIMIT %s
    """
    with conn.cursor() as cur:
        cur.execute(sql, (vec_literal, vec_literal, top_k))
        rows = [
            {
                "id": str(row[0]),
                "title": row[1],
                "program": row[2],
                "similarity": float(row[3]),
            }
            for row in cur.fetchall()
        ]

    print(f"\nQuery: {query!r} (top {top_k})")
    for idx, row in enumerate(rows, start=1):
        prog = f" [{row['program']}]" if row["program"] else ""
        print(f"  {idx}. sim={row['similarity']:.4f} | {row['title']}{prog} ({row['id']})")
    return rows


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Backfill and smoke-test lesson embeddings via Cloudflare Workers AI.")
    parser.add_argument("--database-url", help="PostgreSQL connection URL (overrides DB_URL/DATABASE_URL env vars).")
    parser.add_argument("--env-file", help="Path to extra .env file for CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN.")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"Cloudflare embedding model (default: {DEFAULT_MODEL}).")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE, help=f"Batch size (default: {DEFAULT_BATCH_SIZE}).")
    parser.add_argument("--delay-ms", type=int, default=DEFAULT_DELAY_MS, help=f"Delay between batches in ms (default: {DEFAULT_DELAY_MS}).")
    parser.add_argument("--max-retries", type=int, default=DEFAULT_MAX_RETRIES, help=f"Max retries per batch (default: {DEFAULT_MAX_RETRIES}).")
    parser.add_argument("--limit", type=int, default=None, help="Max pending lessons to process.")
    parser.add_argument("--force", action="store_true", help="Re-embed even when content_hash matches.")
    parser.add_argument("--dry-run", action="store_true", help="Compute stats and pending counts without calling Cloudflare or writing DB.")
    parser.add_argument("--include-all", action="store_true", help="Include non-published/non-done lessons (default: DONE + PUBLISHED only).")
    parser.add_argument("--search", help="Run semantic search smoke test for the given query instead of backfilling.")
    parser.add_argument("--top-k", type=int, default=5, help="Number of search results to show with --search (default: 5).")
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(argv)

    if args.batch_size <= 0:
        parser.error("--batch-size must be positive")

    load_env_files(env_file=args.env_file)

    account_id = os.getenv("CLOUDFLARE_ACCOUNT_ID", "").strip()
    api_token = os.getenv("CLOUDFLARE_API_TOKEN", "").strip()

    def _embed(texts: list[str]) -> list[list[float]]:
        return embed_texts(
            texts,
            account_id=account_id,
            api_token=api_token,
            model=args.model,
            max_retries=args.max_retries,
        )

    conn = connect_db(database_url=args.database_url)
    try:
        if args.search:
            if not account_id or not api_token:
                raise ValueError("Missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN for --search")
            run_semantic_search(conn, args.search, _embed, top_k=args.top_k)
            return 0

        if not args.dry_run and (not account_id or not api_token):
            raise ValueError("Missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN")

        summary = run_backfill(
            conn,
            _embed,
            model=args.model,
            batch_size=args.batch_size,
            delay_ms=args.delay_ms,
            force=args.force,
            dry_run=args.dry_run,
            limit=args.limit,
            include_all=args.include_all,
        )
        print(
            f"Final: total={summary['total']} skipped={summary['skipped']} "
            f"pending={summary['pending']} upserted={summary['upserted']} remaining={summary['remaining']}"
        )
        if summary["rate_limited"]:
            return EXIT_RATE_LIMITED
        if summary["interrupted"]:
            return 130
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
