#!/usr/bin/env python3
"""Classify unclassified lessons into active lesson_categories via Cloudflare Workers AI."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path
from typing import Any, Callable, Sequence

import requests

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from backfill_lesson_embeddings import (  # noqa: E402
    DEFAULT_MAX_RETRIES,
    EXIT_RATE_LIMITED,
    MAX_RETRY_AFTER_SECONDS,
    RateLimitExhaustedError,
    _clean_text,
    _parse_retry_after,
    _summarize_cloudflare_error,
    _truncate_at_word_boundary,
    connect_db,
    load_env_files,
)

DEFAULT_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast"
DEFAULT_MAX_CATEGORIES = 3
DEFAULT_DELAY_MS = 200
MAX_INPUT_CHARS = 6000
SQL_TRANSCRIPT_CAP = 8000


class FatalClassificationError(RuntimeError):
    """Raised on non-retryable API/config errors (e.g. HTTP 400/401/403/404) to fail fast."""


def build_lesson_classification_input(
    title: str | None,
    program: str | None,
    description: str | None,
    transcript: str | None,
    max_chars: int = MAX_INPUT_CHARS,
) -> str:
    """Build concise lesson text for LLM classification, preserving metadata."""
    metadata_lines: list[str] = []
    clean_title = _clean_text(title)
    if clean_title:
        metadata_lines.append(f"Title: {clean_title}")

    clean_program = _clean_text(program)
    if clean_program:
        metadata_lines.append(f"Program: {clean_program}")

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


def build_classification_messages(
    lesson_text: str,
    categories: Sequence[dict[str, Any]],
    max_categories: int = DEFAULT_MAX_CATEGORIES,
) -> list[dict[str, str]]:
    """Build system and user messages for Cloudflare Workers AI chat completion."""
    cat_lines: list[str] = []
    for cat in categories:
        slug = cat["slug"]
        name = _clean_text(cat["name"])
        name_vi = _clean_text(cat.get("name_vi"))
        desc = _clean_text(cat.get("description"))
        label = f"{name} ({name_vi})" if name_vi and name_vi.lower() != name.lower() else name
        detail = f" - {desc}" if desc and desc.lower() not in (name.lower(), name_vi.lower()) else ""
        cat_lines.append(f"- {slug}: {label}{detail}")

    categories_block = "\n".join(cat_lines)
    system_prompt = (
        "You are an expert English learning content classifier. "
        f"Assign 1 to {max_categories} most relevant category slugs from the allowed list below "
        "based on the lesson's title, program, description, and transcript. "
        "Prefer 1 or 2 categories when the topic is focused; use up to "
        f"{max_categories} only when multiple topics are clearly covered. "
        "Return ONLY valid JSON matching the schema."
    )
    user_prompt = (
        f"Allowed categories (slug: name - description):\n{categories_block}\n\n"
        f"Lesson content:\n{lesson_text}"
    )
    return [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ]


def build_classification_schema(
    valid_slugs: Sequence[str],
    max_categories: int = DEFAULT_MAX_CATEGORIES,
) -> dict[str, Any]:
    """Build JSON Schema for Cloudflare Workers AI structured output."""
    return {
        "type": "object",
        "properties": {
            "categories": {
                "type": "array",
                "items": {"type": "string", "enum": list(valid_slugs)},
                "minItems": 1,
                "maxItems": max_categories,
            }
        },
        "required": ["categories"],
        "additionalProperties": False,
    }


def _strip_code_fences(text: str) -> str:
    stripped = text.strip()
    if stripped.startswith("```"):
        lines = stripped.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        return "\n".join(lines).strip()
    return stripped


def parse_classification_response(
    payload: dict[str, Any],
    valid_slugs: set[str],
    max_categories: int = DEFAULT_MAX_CATEGORIES,
) -> list[str]:
    """Extract, validate, and deduplicate category slugs from Cloudflare Workers AI response."""
    result = payload.get("result")
    if not isinstance(result, dict):
        raise ValueError("Missing 'result' object in Cloudflare API response")

    raw_data: Any = None
    if "categories" in result:
        raw_data = result
    elif result.get("response") is not None:
        raw_data = result["response"]
    elif isinstance(result.get("choices"), list) and result["choices"]:
        first_choice = result["choices"][0]
        if isinstance(first_choice, dict):
            msg = first_choice.get("message")
            if isinstance(msg, dict):
                raw_data = msg.get("content")

    if isinstance(raw_data, str):
        cleaned = _strip_code_fences(raw_data)
        try:
            raw_data = json.loads(cleaned)
        except ValueError as exc:
            raise ValueError(f"Model returned non-JSON response: {cleaned[:200]}") from exc

    if not isinstance(raw_data, dict):
        raise ValueError(f"Expected JSON object from model, got {type(raw_data).__name__}")

    raw_categories = raw_data.get("categories")
    if not isinstance(raw_categories, list):
        raise ValueError("Missing 'categories' array in model JSON output")

    selected: list[str] = []
    seen: set[str] = set()
    for item in raw_categories:
        if not isinstance(item, str):
            continue
        slug = item.strip()
        if slug in valid_slugs and slug not in seen:
            seen.add(slug)
            selected.append(slug)
            if len(selected) >= max_categories:
                break

    if not selected:
        raise ValueError(f"No valid category slugs returned (raw={raw_categories!r})")
    return selected


def classify_lesson_text(
    lesson_text: str,
    categories: Sequence[dict[str, Any]],
    account_id: str,
    api_token: str,
    model: str = DEFAULT_MODEL,
    max_categories: int = DEFAULT_MAX_CATEGORIES,
    timeout: int = 120,
    max_retries: int = DEFAULT_MAX_RETRIES,
    sleep_fn: Callable[[float], None] = time.sleep,
    post_fn: Callable[..., Any] = requests.post,
) -> list[str]:
    """Classify one lesson using Cloudflare Workers AI REST API with retry and 429 handling."""
    if not account_id or not api_token:
        raise FatalClassificationError("Missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN")
    if not categories:
        raise FatalClassificationError("No active categories available for classification")

    valid_slugs = [cat["slug"] for cat in categories]
    valid_slug_set = set(valid_slugs)

    url = f"https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/run/{model}"
    headers = {
        "Authorization": f"Bearer {api_token}",
        "Content-Type": "application/json",
    }
    body = {
        "messages": build_classification_messages(
            lesson_text=lesson_text,
            categories=categories,
            max_categories=max_categories,
        ),
        "temperature": 0.1,
        "max_tokens": 128,
        "response_format": {
            "type": "json_schema",
            "json_schema": build_classification_schema(valid_slugs, max_categories=max_categories),
        },
    }

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
            delay = (
                retry_after
                if (retry_after is not None and retry_after > 0)
                else float(min(2 ** (attempt + 1), 30))
            )
            sleep_fn(delay)
            continue

        if resp.status_code in (500, 502, 503, 504):
            if attempt >= max_retries:
                raise RuntimeError(f"Cloudflare API HTTP {resp.status_code}: {err_summary}")
            sleep_fn(float(min(2 ** (attempt + 1), 30)))
            continue

        if resp.status_code >= 400:
            raise FatalClassificationError(f"Cloudflare API HTTP {resp.status_code}: {err_summary}")

        if not payload or payload.get("success") is not True:
            if is_quota:
                raise RateLimitExhaustedError(f"Cloudflare quota exhausted: {err_summary}")
            raise RuntimeError(f"Cloudflare API returned failure payload: {err_summary}")

        return parse_classification_response(
            payload,
            valid_slugs=valid_slug_set,
            max_categories=max_categories,
        )

    raise RateLimitExhaustedError("Cloudflare rate limit retries exhausted")


def fetch_active_categories(conn) -> list[dict[str, Any]]:
    """Fetch active, non-deleted lesson categories."""
    sql = """
        SELECT
            id::text,
            name,
            name_vi,
            slug,
            description
        FROM public.lesson_categories
        WHERE is_deleted = false
          AND status = 'ACTIVE'
        ORDER BY sort_order ASC, name ASC
    """
    with conn.cursor() as cur:
        cur.execute(sql)
        return [
            {
                "id": str(row[0]),
                "name": str(row[1]),
                "name_vi": row[2],
                "slug": str(row[3]),
                "description": row[4],
            }
            for row in cur.fetchall()
        ]


def fetch_unclassified_lessons(
    conn,
    include_all: bool = False,
    force: bool = False,
) -> list[dict[str, Any]]:
    """Fetch lessons without active categories (or all lessons when force=True)."""
    visibility_filter = "" if include_all else "AND l.status = 'DONE' AND l.visibility_status = 'PUBLISHED'"
    unclassified_filter = (
        ""
        if force
        else """
          AND NOT EXISTS (
              SELECT 1
              FROM public.lesson_category_lesson lcl
              JOIN public.lesson_categories c ON c.id = lcl.category_id
              WHERE lcl.lesson_id = l.id
                AND c.is_deleted = false
                AND c.status = 'ACTIVE'
          )
        """
    )
    sql = f"""
        SELECT
            l.id::text AS lesson_id,
            l.title,
            l.program,
            l.description,
            LEFT(
                COALESCE(
                    (
                        SELECT string_agg(s.text, ' ' ORDER BY s.display_order ASC, s.start_time ASC)
                        FROM public.transcription_segments s
                        WHERE s.lesson_id = l.id
                          AND s.is_deleted = false
                    ),
                    ''
                ),
                {SQL_TRANSCRIPT_CAP}
            ) AS transcript
        FROM public.lessons l
        WHERE l.is_deleted = false
          {visibility_filter}
          {unclassified_filter}
        ORDER BY l.id ASC
    """
    with conn.cursor() as cur:
        cur.execute(sql)
        return [
            {
                "id": str(lesson_id),
                "title": title,
                "program": program,
                "description": description,
                "transcript": transcript,
            }
            for lesson_id, title, program, description, transcript in cur.fetchall()
        ]


def save_lesson_categories(
    conn,
    lesson_id: str,
    category_ids: Sequence[str],
    replace: bool = False,
) -> None:
    """Persist lesson-category links and commit immediately for crash/429 resumability."""
    if not category_ids:
        return
    with conn.cursor() as cur:
        if replace:
            cur.execute(
                "DELETE FROM public.lesson_category_lesson WHERE lesson_id = %s::uuid",
                (lesson_id,),
            )
        cur.executemany(
            """
            INSERT INTO public.lesson_category_lesson (lesson_id, category_id)
            VALUES (%s::uuid, %s::uuid)
            ON CONFLICT (lesson_id, category_id) DO NOTHING
            """,
            [(lesson_id, cat_id) for cat_id in category_ids],
        )
    conn.commit()


def run_classification(
    conn,
    classify_fn: Callable[[str, Sequence[dict[str, Any]]], list[str]],
    *,
    delay_ms: int = DEFAULT_DELAY_MS,
    force: bool = False,
    dry_run: bool = False,
    limit: int | None = None,
    include_all: bool = False,
    exclude_slugs: Sequence[str] = (),
    sleep_fn: Callable[[float], None] = time.sleep,
) -> dict[str, int]:
    """Run resumable classification for unclassified lessons."""
    excluded = {s.strip() for s in exclude_slugs if s and s.strip()}
    categories = [
        cat for cat in fetch_active_categories(conn)
        if cat["slug"] not in excluded
    ]
    if not categories:
        raise ValueError("No active lesson_categories available after filtering")

    slug_to_category = {cat["slug"]: cat for cat in categories}
    lessons = fetch_unclassified_lessons(conn, include_all=include_all, force=force)

    pending_items: list[dict[str, str]] = []
    skipped = 0
    for lesson in lessons:
        lesson_text = build_lesson_classification_input(
            title=lesson.get("title"),
            program=lesson.get("program"),
            description=lesson.get("description"),
            transcript=lesson.get("transcript"),
        )
        if not lesson_text:
            skipped += 1
            continue
        pending_items.append(
            {
                "lesson_id": lesson["id"],
                "title": _clean_text(lesson.get("title")) or lesson["id"],
                "lesson_text": lesson_text,
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
        "classified": 0,
        "failed": 0,
        "remaining": pending,
        "rate_limited": 0,
        "interrupted": 0,
    }

    if dry_run or pending == 0:
        if dry_run:
            cat_preview = ", ".join(f"{c['slug']} ({c['name']})" for c in categories)
            print(f"[DRY RUN] Active categories ({len(categories)}): {cat_preview}")
            if pending_items:
                max_len = max(len(item["lesson_text"]) for item in pending_items)
                avg_len = sum(len(item["lesson_text"]) for item in pending_items) / len(pending_items)
                print(
                    f"[DRY RUN] total={total} skipped={skipped} pending={pending} "
                    f"avg_chars={avg_len:.1f} max_chars={max_len}"
                )
                print(
                    f"[DRY RUN] Sample[0] ({pending_items[0]['lesson_id']}): "
                    f"{pending_items[0]['lesson_text'][:200]}..."
                )
            else:
                print(f"[DRY RUN] total={total} skipped={skipped} pending=0")
        else:
            print(
                f"Summary: categories={len(categories)} total={total} skipped={skipped} "
                f"pending={pending} classified=0"
            )
        return summary

    for idx, item in enumerate(pending_items):
        try:
            selected_slugs = classify_fn(item["lesson_text"], categories)
            category_ids = [slug_to_category[s]["id"] for s in selected_slugs if s in slug_to_category]
            if not category_ids:
                raise ValueError(f"No matching category IDs for slugs={selected_slugs!r}")

            save_lesson_categories(conn, item["lesson_id"], category_ids, replace=force)
            summary["classified"] += 1
            summary["remaining"] = pending - summary["classified"] - summary["failed"]
            print(
                f"[{idx + 1}/{pending}] {item['title']} ({item['lesson_id']}) -> "
                f"{', '.join(selected_slugs)}"
            )
            if delay_ms > 0 and idx + 1 < pending:
                sleep_fn(delay_ms / 1000.0)
        except RateLimitExhaustedError as exc:
            conn.rollback()
            summary["rate_limited"] = 1
            print(
                f"Paused due to Cloudflare rate limit/quota: {exc}\n"
                f"Progress saved: Classified={summary['classified']}, Failed={summary['failed']}, "
                f"Remaining={summary['remaining']}. Re-run the exact same command later to resume.",
                file=sys.stderr,
            )
            break
        except KeyboardInterrupt:
            conn.rollback()
            summary["interrupted"] = 1
            print(
                f"\nInterrupted by user. Progress saved: Classified={summary['classified']}, "
                f"Failed={summary['failed']}, Remaining={summary['remaining']}. "
                f"Re-run the exact same command later to resume.",
                file=sys.stderr,
            )
            break
        except FatalClassificationError:
            conn.rollback()
            raise
        except (ValueError, RuntimeError) as exc:
            conn.rollback()
            summary["failed"] += 1
            summary["remaining"] = pending - summary["classified"] - summary["failed"]
            print(
                f"[{idx + 1}/{pending}] FAILED {item['title']} ({item['lesson_id']}): {exc}",
                file=sys.stderr,
            )

    return summary


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Classify unclassified lessons into active lesson_categories via Cloudflare Workers AI."
    )
    parser.add_argument("--database-url", help="PostgreSQL connection URL (overrides DB_URL/DATABASE_URL env vars).")
    parser.add_argument("--env-file", help="Path to extra .env file for CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN.")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"Cloudflare text-generation model (default: {DEFAULT_MODEL}).")
    parser.add_argument(
        "--max-categories",
        type=int,
        default=DEFAULT_MAX_CATEGORIES,
        help=f"Max categories assigned per lesson (default: {DEFAULT_MAX_CATEGORIES}).",
    )
    parser.add_argument(
        "--exclude-slugs",
        default="",
        help="Comma-separated category slugs to exclude from classification candidates.",
    )
    parser.add_argument("--delay-ms", type=int, default=DEFAULT_DELAY_MS, help=f"Delay between requests in ms (default: {DEFAULT_DELAY_MS}).")
    parser.add_argument("--max-retries", type=int, default=DEFAULT_MAX_RETRIES, help=f"Max retries per lesson (default: {DEFAULT_MAX_RETRIES}).")
    parser.add_argument("--limit", type=int, default=None, help="Max unclassified lessons to process.")
    parser.add_argument("--force", action="store_true", help="Re-classify all lessons and replace existing category links.")
    parser.add_argument("--dry-run", action="store_true", help="Print active categories, stats, and sample input without calling Cloudflare or writing DB.")
    parser.add_argument("--include-all", action="store_true", help="Include non-published/non-done lessons (default: DONE + PUBLISHED only).")
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(argv)

    if args.max_categories <= 0:
        parser.error("--max-categories must be positive")
    if args.delay_ms < 0:
        parser.error("--delay-ms must be non-negative")
    if args.max_retries < 0:
        parser.error("--max-retries must be non-negative")
    if args.limit is not None and args.limit < 0:
        parser.error("--limit must be non-negative")

    load_env_files(env_file=args.env_file)

    account_id = os.getenv("CLOUDFLARE_ACCOUNT_ID", "").strip()
    api_token = os.getenv("CLOUDFLARE_API_TOKEN", "").strip()

    if not args.dry_run and (not account_id or not api_token):
        raise ValueError("Missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN")

    exclude_slugs = [s.strip() for s in args.exclude_slugs.split(",") if s.strip()]

    conn = connect_db(database_url=args.database_url)
    try:
        with requests.Session() as session:
            def _classify(lesson_text: str, categories: Sequence[dict[str, Any]]) -> list[str]:
                return classify_lesson_text(
                    lesson_text,
                    categories,
                    account_id=account_id,
                    api_token=api_token,
                    model=args.model,
                    max_categories=args.max_categories,
                    max_retries=args.max_retries,
                    post_fn=session.post,
                )

            summary = run_classification(
                conn,
                _classify,
                delay_ms=args.delay_ms,
                force=args.force,
                dry_run=args.dry_run,
                limit=args.limit,
                include_all=args.include_all,
                exclude_slugs=exclude_slugs,
            )

        print(
            f"Final: total={summary['total']} skipped={summary['skipped']} "
            f"pending={summary['pending']} classified={summary['classified']} "
            f"failed={summary['failed']} remaining={summary['remaining']}"
        )
        if summary["rate_limited"]:
            return EXIT_RATE_LIMITED
        if summary["interrupted"]:
            return 130
        if summary["failed"] > 0:
            return 1
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
