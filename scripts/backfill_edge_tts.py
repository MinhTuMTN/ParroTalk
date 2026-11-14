"""Async/concurrent Edge-TTS backfill script for vocabulary_pronunciations.

Replaces missing or blocked (dictionary.cambridge.org) audio_url entries by:
1. Checking tts_audio_cache first for an existing Cloudinary URL.
2. Synthesizing MP3 audio via Edge TTS and uploading to Cloudinary concurrently.
3. Upserting tts_audio_cache and updating vocabulary_pronunciations.audio_url.
"""

from __future__ import annotations

import argparse
import asyncio
from dataclasses import dataclass
import os
from pathlib import Path
import sys
import time
from typing import Any
from urllib.parse import urlparse

import psycopg2
import psycopg2.extras

try:
    from dotenv import load_dotenv
except ImportError:  # pragma: no cover - optional when env vars are pre-set
    load_dotenv = None

REPO_ROOT = Path(__file__).resolve().parent.parent
AUDIO_SERVICE_DIR = REPO_ROOT / "parro-talk-audio-service"
if str(AUDIO_SERVICE_DIR) not in sys.path:
    sys.path.insert(0, str(AUDIO_SERVICE_DIR))

from app.tts import (  # noqa: E402
    compute_tts_cache_key,
    normalize_accent,
    resolve_voice,
    synthesize_and_upload_batch_async,
    validate_and_clean_text,
)


@dataclass(frozen=True)
class PronunciationTarget:
    pronunciation_id: str
    word: str
    cleaned_text: str
    accent: str
    voice: str
    cache_key: str


def _load_env_files() -> None:
    if load_dotenv is None:
        return
    for env_path in (AUDIO_SERVICE_DIR / ".env", REPO_ROOT / ".env"):
        if env_path.is_file():
            load_dotenv(env_path, override=False)


def _build_db_conn() -> psycopg2.extensions.connection:
    database_url = os.getenv("DB_URL") or os.getenv("DATABASE_URL")
    if database_url:
        if database_url.startswith("jdbc:"):
            database_url = database_url[len("jdbc:") :]
        parsed = urlparse(database_url)
        if parsed.scheme.startswith("postgres"):
            return psycopg2.connect(
                host=parsed.hostname or os.getenv("PGHOST", "localhost"),
                port=parsed.port or int(os.getenv("PGPORT", "5432")),
                dbname=(parsed.path or "/parrotalk").lstrip("/"),
                user=parsed.username or os.getenv("DB_USERNAME", os.getenv("PGUSER", "admin")),
                password=parsed.password or os.getenv("DB_PASSWORD", os.getenv("PGPASSWORD", "123456")),
            )

    return psycopg2.connect(
        host=os.getenv("PGHOST", os.getenv("DB_HOST", "localhost")),
        port=int(os.getenv("PGPORT", os.getenv("DB_PORT", "5432"))),
        dbname=os.getenv("PGDATABASE", os.getenv("DB_NAME", "parrotalk")),
        user=os.getenv("DB_USERNAME", os.getenv("PGUSER", os.getenv("DB_USER", "postgres"))),
        password=os.getenv("DB_PASSWORD", os.getenv("PGPASSWORD", os.getenv("DB_PASSWORD", "postgres"))),
    )


def fetch_pronunciations_needing_tts(
    conn: psycopg2.extensions.connection,
    limit: int | None = None,
) -> list[tuple[str, str, str]]:
    sql = """
        SELECT p.id::text, v.word, p.accent
        FROM vocabulary_pronunciations p
        JOIN vocabularies v ON v.id = p.vocabulary_id
        WHERE p.is_deleted = false
          AND v.is_deleted = false
          AND (
              p.audio_url IS NULL
              OR BTRIM(p.audio_url) = ''
              OR STRPOS(LOWER(p.audio_url), 'dictionary.cambridge.org') > 0
          )
        ORDER BY v.normalized_word ASC, p.accent ASC
    """
    if limit is not None and limit > 0:
        sql += " LIMIT %s"
    with conn.cursor() as cur:
        if limit is not None and limit > 0:
            cur.execute(sql, (limit,))
        else:
            cur.execute(sql)
        return [(row[0], row[1], row[2]) for row in cur.fetchall()]


def prepare_targets(
    raw_rows: list[tuple[str, str, str]],
) -> tuple[list[PronunciationTarget], int]:
    targets: list[PronunciationTarget] = []
    skipped = 0
    for pid, word, accent in raw_rows:
        if not word or not word.strip():
            skipped += 1
            continue
        try:
            cleaned = validate_and_clean_text(word)
            norm_accent = normalize_accent(accent)
            voice = resolve_voice(norm_accent)
            cache_key = compute_tts_cache_key(cleaned, norm_accent, voice)
            targets.append(
                PronunciationTarget(
                    pronunciation_id=pid,
                    word=word,
                    cleaned_text=cleaned,
                    accent=norm_accent,
                    voice=voice,
                    cache_key=cache_key,
                )
            )
        except Exception:
            skipped += 1
    return targets, skipped


def fetch_existing_cache_urls(
    conn: psycopg2.extensions.connection,
    cache_keys: list[str],
) -> dict[str, str]:
    if not cache_keys:
        return {}
    sql = """
        SELECT cache_key, cloudinary_url
        FROM tts_audio_cache
        WHERE cache_key = ANY(%s)
          AND cloudinary_url IS NOT NULL
          AND BTRIM(cloudinary_url) <> ''
    """
    with conn.cursor() as cur:
        cur.execute(sql, (cache_keys,))
        return {row[0]: row[1] for row in cur.fetchall()}


def persist_results(
    conn: psycopg2.extensions.connection,
    cache_rows: list[tuple[str, str, str, str]],
    pronunciation_updates: list[tuple[str, str]],
) -> None:
    with conn.cursor() as cur:
        if cache_rows:
            psycopg2.extras.execute_values(
                cur,
                """
                INSERT INTO tts_audio_cache (cache_key, cloudinary_url, accent, voice, created_at)
                VALUES %s
                ON CONFLICT (cache_key) DO UPDATE
                SET cloudinary_url = EXCLUDED.cloudinary_url
                """,
                cache_rows,
                template="(%s, %s, %s, %s, NOW())",
                page_size=200,
            )
        if pronunciation_updates:
            psycopg2.extras.execute_values(
                cur,
                """
                UPDATE vocabulary_pronunciations AS p
                SET audio_url = data.audio_url,
                    updated_at = NOW()
                FROM (VALUES %s) AS data(id, audio_url)
                WHERE p.id = data.id::uuid
                """,
                pronunciation_updates,
                page_size=200,
            )
    conn.commit()


def build_unique_synthesis_items(
    targets: list[PronunciationTarget],
    cached_urls: dict[str, str],
) -> list[dict[str, Any]]:
    seen_keys: set[str] = set()
    items: list[dict[str, Any]] = []
    for t in targets:
        if t.cache_key in cached_urls or t.cache_key in seen_keys:
            continue
        seen_keys.add(t.cache_key)
        items.append(
            {
                "text": t.cleaned_text,
                "accent": t.accent,
                "voice": t.voice,
                "cacheKey": t.cache_key,
            }
        )
    return items


async def run_backfill_async(
    concurrency: int,
    batch_size: int,
    limit: int | None,
    dry_run: bool,
) -> None:
    _load_env_files()
    started = time.perf_counter()
    conn = _build_db_conn()
    try:
        raw_rows = fetch_pronunciations_needing_tts(conn, limit=limit)
        targets, skipped = prepare_targets(raw_rows)
        print(
            f"Scanned {len(raw_rows)} pronunciation rows needing TTS "
            f"(valid={len(targets)}, skipped={skipped})."
        )
        if not targets:
            return

        all_keys = list({t.cache_key for t in targets})
        cached_urls = fetch_existing_cache_urls(conn, all_keys)
        to_synthesize = build_unique_synthesis_items(targets, cached_urls)

        print(
            f"Cache hits: {sum(1 for t in targets if t.cache_key in cached_urls)} rows; "
            f"unique items to synthesize & upload: {len(to_synthesize)}."
        )

        if dry_run:
            for item in to_synthesize[:10]:
                print(f"[DRY-RUN] {item['accent']} | {item['text']} -> {item['cacheKey']}")
            return

        # First apply immediate cache hits
        cache_hit_updates = [
            (t.pronunciation_id, cached_urls[t.cache_key])
            for t in targets
            if t.cache_key in cached_urls
        ]
        if cache_hit_updates:
            persist_results(conn, [], cache_hit_updates)
            print(f"Updated {len(cache_hit_updates)} rows directly from tts_audio_cache.")

        # Process missing items in batches with bounded async concurrency
        total_synthesized = 0
        total_updated = len(cache_hit_updates)
        targets_by_key: dict[str, list[PronunciationTarget]] = {}
        for t in targets:
            if t.cache_key not in cached_urls:
                targets_by_key.setdefault(t.cache_key, []).append(t)

        for idx in range(0, len(to_synthesize), batch_size):
            batch = to_synthesize[idx : idx + batch_size]
            results = await synthesize_and_upload_batch_async(batch, concurrency=concurrency)
            cache_rows: list[tuple[str, str, str, str]] = []
            pron_updates: list[tuple[str, str]] = []
            for res in results:
                key = res["cacheKey"]
                url = res["cloudinaryUrl"]
                cache_rows.append((key, url, res["accent"], res["voice"]))
                for t in targets_by_key.get(key, []):
                    pron_updates.append((t.pronunciation_id, url))
            if cache_rows or pron_updates:
                persist_results(conn, cache_rows, pron_updates)
            total_synthesized += len(results)
            total_updated += len(pron_updates)
            print(
                f"Batch {idx // batch_size + 1}: synthesized={len(results)}/{len(batch)}, "
                f"updated_rows={len(pron_updates)}"
            )

        elapsed = time.perf_counter() - started
        print(
            f"Completed Edge-TTS backfill: synthesized={total_synthesized}, "
            f"updated_rows={total_updated}, elapsed={elapsed:.2f}s"
        )
    finally:
        conn.close()


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Async/concurrent Edge-TTS backfill for vocabulary_pronunciations."
    )
    parser.add_argument("--concurrency", type=int, default=8)
    parser.add_argument("--batch-size", type=int, default=50)
    parser.add_argument("--limit", type=int, default=None)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    if args.concurrency < 1:
        parser.error("--concurrency must be >= 1")
    if args.batch_size < 1:
        parser.error("--batch-size must be >= 1")
    return args


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    asyncio.run(
        run_backfill_async(
            concurrency=args.concurrency,
            batch_size=args.batch_size,
            limit=args.limit,
            dry_run=args.dry_run,
        )
    )


if __name__ == "__main__":
    main()
