#!/usr/bin/env python3
"""Backfill vocabularies.word_forms using lemminflect inflections."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Any


def compute_word_forms(
    word: str,
    normalized_word: str | None = None,
    get_all_inflections: Any = None,
) -> list[str]:
    """Compute sorted, deduplicated lowercase inflections excluding normalized_word."""
    if get_all_inflections is None:
        from lemminflect import getAllInflections

        get_all_inflections = getAllInflections

    norm = (normalized_word if normalized_word is not None else word).strip().lower()
    lookup = word.strip().lower()
    if not lookup and not norm:
        return []

    inflections: dict[str, tuple[str, ...]] = {}
    if lookup:
        inflections.update(get_all_inflections(lookup))
    if norm and norm != lookup:
        for tag, forms in get_all_inflections(norm).items():
            inflections.setdefault(tag, forms)

    forms_set: set[str] = set()
    for forms in inflections.values():
        for form in forms:
            cleaned = form.strip().lower()
            if cleaned and cleaned != norm:
                forms_set.add(cleaned)

    return sorted(forms_set)


def connect_db():
    """Connect to PostgreSQL using DATABASE_URL or PG* environment variables."""
    try:
        from dotenv import load_dotenv

        load_dotenv()
    except ImportError:
        pass

    import psycopg2

    database_url = os.getenv("DATABASE_URL")
    if database_url:
        return psycopg2.connect(database_url)

    conn_kwargs: dict[str, Any] = {}
    for env_key, kw_key in (
        ("PGHOST", "host"),
        ("PGPORT", "port"),
        ("PGDATABASE", "dbname"),
        ("PGUSER", "user"),
        ("PGPASSWORD", "password"),
    ):
        val = os.getenv(env_key)
        if val:
            conn_kwargs[kw_key] = val

    return psycopg2.connect(**conn_kwargs)


def run_sample(words: list[str], dry_run: bool) -> int:
    """Compute and print inflections for sample words without a DB connection."""
    from lemminflect import getAllInflections

    start = time.perf_counter()
    for raw_word in words:
        word = raw_word.strip()
        if not word:
            continue
        forms = compute_word_forms(word, get_all_inflections=getAllInflections)
        print(f"{word} -> {json.dumps(forms)}")

    elapsed = time.perf_counter() - start
    mode = "dry-run sample" if dry_run else "sample"
    print(f"Summary ({mode}): scanned={len(words)}, updated=0, elapsed={elapsed:.3f}s")
    return 0


def chunked(items: list[tuple[str, str]], size: int):
    """Yield successive chunks of given size."""
    for i in range(0, len(items), size):
        yield items[i : i + size]


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Backfill vocabularies.word_forms using lemminflect."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Compute inflections without writing updates to PostgreSQL.",
    )
    parser.add_argument(
        "--only-empty",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Only process rows where word_forms is NULL or empty (default: true).",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=min(32, (os.cpu_count() or 4) * 2),
        help="Number of worker threads for inflection computation.",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=500,
        help="Number of rows per PostgreSQL batch update.",
    )
    parser.add_argument(
        "--sample",
        type=str,
        default=None,
        help="Comma-separated sample words to test without DB (e.g. story,find,child,amuse).",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)

    if args.workers < 1:
        print("--workers must be >= 1", file=sys.stderr)
        return 2
    if args.batch_size < 1:
        print("--batch-size must be >= 1", file=sys.stderr)
        return 2

    if args.sample is not None:
        sample_words = [w.strip() for w in args.sample.split(",") if w.strip()]
        return run_sample(sample_words, dry_run=args.dry_run)

    from lemminflect import getAllInflections
    from psycopg2.extras import execute_values

    start = time.perf_counter()
    scanned = 0
    updated = 0

    conn = connect_db()
    try:
        query = (
            "SELECT id::text, word, normalized_word "
            "FROM vocabularies "
            "WHERE is_deleted = FALSE"
        )
        if args.only_empty:
            query += " AND (word_forms IS NULL OR word_forms = '[]'::jsonb)"

        with conn.cursor() as cur:
            cur.execute(query)
            rows = cur.fetchall()

        scanned = len(rows)

        def _process_row(row: tuple[str, str, str | None]) -> tuple[str, str]:
            row_id, word, normalized_word = row
            forms = compute_word_forms(
                word,
                normalized_word=normalized_word,
                get_all_inflections=getAllInflections,
            )
            return row_id, json.dumps(forms)

        with ThreadPoolExecutor(max_workers=args.workers) as executor:
            payloads = list(executor.map(_process_row, rows))

        if args.dry_run:
            for row_id, forms_json in payloads[:10]:
                print(f"[dry-run] {row_id} -> {forms_json}")
            if len(payloads) > 10:
                print(f"[dry-run] ... and {len(payloads) - 10} more rows")
        else:
            update_sql = (
                "UPDATE vocabularies AS v "
                "SET word_forms = CAST(data.forms AS jsonb), "
                "updated_at = NOW() "
                "FROM (VALUES %s) AS data(id, forms) "
                "WHERE v.id = data.id::uuid"
            )
            with conn.cursor() as cur:
                for batch in chunked(payloads, args.batch_size):
                    execute_values(cur, update_sql, batch, page_size=args.batch_size)
                    updated += len(batch)
                conn.commit()
    finally:
        conn.close()

    elapsed = time.perf_counter() - start
    print(f"Summary: scanned={scanned}, updated={updated}, elapsed={elapsed:.3f}s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
