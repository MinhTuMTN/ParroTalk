#!/usr/bin/env python3
"""Unit tests for scripts/backfill_lesson_embeddings.py."""

from __future__ import annotations

import math
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock

import pytest
import requests

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import backfill_lesson_embeddings as ble


def _make_response(status_code: int, payload: dict | None = None, headers: dict | None = None, text: str = ""):
    resp = MagicMock()
    resp.status_code = status_code
    resp.headers = headers or {}
    resp.text = text
    if payload is not None:
        resp.json.return_value = payload
    else:
        resp.json.side_effect = ValueError("No JSON body")
    return resp


def test_build_search_content_full_and_bilingual_categories():
    content = ble.build_search_content(
        title="  Preserving traditional recipes  ",
        program="BBC 6 Minute English",
        categories=[
            ("Food & Restaurants", "Thực phẩm & Nhà hàng"),
            ("Culture & Society", "Văn hóa & Xã hội"),
            ("Culture & Society", "Văn hóa & Xã hội"),
        ],
        description="A lesson about   cooking traditions.",
        transcript="Hello.\n\nThis is   6 Minute English.",
    )

    lines = content.splitlines()
    assert lines == [
        "Title: Preserving traditional recipes",
        "Program: BBC 6 Minute English",
        "Categories: Culture & Society (Văn hóa & Xã hội), Food & Restaurants (Thực phẩm & Nhà hàng)",
        "Description: A lesson about cooking traditions.",
        "Transcript: Hello. This is 6 Minute English.",
    ]


def test_build_search_content_omits_null_fields_and_null_name_vi():
    content = ble.build_search_content(
        title="Finding Nemo",
        program=None,
        categories=[
            ("6 Minutes English", None),
            ("Environment & Nature", "Environment & Nature"),
            (None, "Ignored"),
        ],
        description=None,
        transcript="Just keep swimming.",
    )

    assert "None" not in content
    assert "()" not in content
    lines = content.splitlines()
    assert lines == [
        "Title: Finding Nemo",
        "Categories: 6 Minutes English, Environment & Nature",
        "Transcript: Just keep swimming.",
    ]


def test_build_search_content_omits_categories_when_empty():
    content = ble.build_search_content(
        title="Movie Clip #1",
        program=None,
        categories=[],
        description="   ",
        transcript="Hello world.",
    )

    assert content == "Title: Movie Clip #1\nTranscript: Hello world."


def test_build_search_content_truncates_long_transcript_preserving_metadata():
    long_transcript = " ".join(f"word{i}" for i in range(4000))
    content = ble.build_search_content(
        title="Very Long Lesson",
        program="BBC 6 Minute English",
        categories=[("Science", "Khoa học")],
        description="Important summary",
        transcript=long_transcript,
        max_chars=300,
    )

    assert len(content) <= 300
    assert content.startswith(
        "Title: Very Long Lesson\n"
        "Program: BBC 6 Minute English\n"
        "Categories: Science (Khoa học)\n"
        "Description: Important summary\n"
        "Transcript: "
    )
    # Truncation should land on a word boundary when spaces exist.
    last_token = content.rsplit(" ", 1)[-1]
    assert last_token.startswith("word")
    assert last_token[4:].isdigit()


def test_compute_content_hash_changes_with_model_or_content():
    h1 = ble.compute_content_hash("@cf/baai/bge-m3", "Title: Demo")
    h2 = ble.compute_content_hash("@cf/baai/bge-m3", "Title: Demo")
    h3 = ble.compute_content_hash("@cf/baai/bge-m3", "Title: Changed")
    h4 = ble.compute_content_hash("@cf/baai/bge-base-en-v1.5", "Title: Demo")

    assert len(h1) == 64
    assert h1 == h2
    assert h1 != h3
    assert h1 != h4


def test_validate_embeddings_accepts_1024_and_rejects_bad_dims_nan_bool():
    valid_vec = [0.1] * ble.EXPECTED_DIMS
    validated = ble.validate_embeddings([valid_vec], expected_count=1)
    assert len(validated) == 1
    assert len(validated[0]) == ble.EXPECTED_DIMS
    assert isinstance(validated[0][0], float)

    with pytest.raises(ValueError):
        ble.validate_embeddings([valid_vec], expected_count=2)

    with pytest.raises(ValueError):
        ble.validate_embeddings([[0.1] * 768], expected_count=1)

    bad_nan = [0.1] * (ble.EXPECTED_DIMS - 1) + [math.nan]
    with pytest.raises(ValueError):
        ble.validate_embeddings([bad_nan], expected_count=1)

    bad_inf = [0.1] * (ble.EXPECTED_DIMS - 1) + [math.inf]
    with pytest.raises(ValueError):
        ble.validate_embeddings([bad_inf], expected_count=1)

    bad_bool = [0.1] * (ble.EXPECTED_DIMS - 1) + [True]
    with pytest.raises(ValueError):
        ble.validate_embeddings([bad_bool], expected_count=1)


def test_parse_db_connect_kwargs_cli_overrides_env_and_strips_jdbc():
    env = {
        "DB_URL": "jdbc:postgresql://env-host:5432/envdb",
        "DATABASE_URL": "postgresql://other:pw@other-host:5433/otherdb",
        "DB_USERNAME": "fallback_user",
        "DB_PASSWORD": "fallback_password",
    }

    cli_kwargs = ble.parse_db_connect_kwargs(
        database_url="jdbc:postgresql://admin:secret@100.114.196.98:5432/parrotalk",
        env=env,
    )
    assert cli_kwargs == {
        "host": "100.114.196.98",
        "port": 5432,
        "dbname": "parrotalk",
        "user": "admin",
        "password": "secret",
    }

    env_kwargs = ble.parse_db_connect_kwargs(database_url=None, env=env)
    assert env_kwargs == {
        "host": "env-host",
        "port": 5432,
        "dbname": "envdb",
        "user": "fallback_user",
        "password": "fallback_password",
    }


def test_load_env_file_only_loads_cloudflare_keys_not_database_url(tmp_path: Path, monkeypatch):
    root_env = tmp_path / "root.env"
    root_env.write_text("DATABASE_URL=postgresql://root:pw@localhost:5432/parrotalk\n", encoding="utf-8")

    tutorial_env = tmp_path / "tutorial.env"
    tutorial_env.write_text(
        "DATABASE_URL=postgresql://postgres:postgres@localhost:5433/vector_poc\n"
        "CLOUDFLARE_ACCOUNT_ID=acc-123\n"
        "CLOUDFLARE_API_TOKEN=tok-456\n",
        encoding="utf-8",
    )

    monkeypatch.delenv("DATABASE_URL", raising=False)
    monkeypatch.delenv("CLOUDFLARE_ACCOUNT_ID", raising=False)
    monkeypatch.delenv("CLOUDFLARE_API_TOKEN", raising=False)

    ble.load_env_files(env_file=str(tutorial_env), default_env_paths=[root_env])

    assert os.environ["DATABASE_URL"] == "postgresql://root:pw@localhost:5432/parrotalk"
    assert os.environ["CLOUDFLARE_ACCOUNT_ID"] == "acc-123"
    assert os.environ["CLOUDFLARE_API_TOKEN"] == "tok-456"


def test_embed_texts_retries_transient_429_and_5xx_then_succeeds():
    vec = [0.25] * ble.EXPECTED_DIMS
    responses = iter(
        [
            _make_response(429, {"success": False, "errors": [{"message": "Rate limited"}]}, headers={"Retry-After": "1"}),
            _make_response(503, {"success": False, "errors": [{"message": "Unavailable"}]}),
            _make_response(200, {"success": True, "result": {"data": [vec]}}),
        ]
    )
    sleeps: list[float] = []
    captured_payloads: list[dict] = []

    def fake_post(url, headers=None, json=None, timeout=None):
        captured_payloads.append({"url": url, "headers": headers, "json": json, "timeout": timeout})
        return next(responses)

    result = ble.embed_texts(
        ["hello"],
        account_id="acc",
        api_token="tok",
        max_retries=4,
        sleep_fn=sleeps.append,
        post_fn=fake_post,
    )

    assert result == [vec]
    assert sleeps == [1.0, 4.0]
    assert captured_payloads[0]["json"] == {"text": ["hello"], "truncate_inputs": True}
    assert captured_payloads[0]["url"] == "https://api.cloudflare.com/client/v4/accounts/acc/ai/run/@cf/baai/bge-m3"


def test_embed_texts_raises_rate_limit_exhausted_on_long_retry_after_or_persistent_429():
    sleeps: list[float] = []

    def post_long_retry_after(url, headers=None, json=None, timeout=None):
        return _make_response(
            429,
            {"success": False, "errors": [{"code": 3036, "message": "Daily neurons limit reached"}]},
            headers={"Retry-After": "3600"},
        )

    with pytest.raises(ble.RateLimitExhaustedError):
        ble.embed_texts(
            ["hello"],
            account_id="acc",
            api_token="tok",
            max_retries=4,
            sleep_fn=sleeps.append,
            post_fn=post_long_retry_after,
        )
    assert sleeps == []

    def post_always_429(url, headers=None, json=None, timeout=None):
        return _make_response(429, {"success": False, "errors": [{"message": "Too many requests"}]})

    with pytest.raises(ble.RateLimitExhaustedError):
        ble.embed_texts(
            ["hello"],
            account_id="acc",
            api_token="tok",
            max_retries=2,
            sleep_fn=sleeps.append,
            post_fn=post_always_429,
        )
    assert sleeps == [2.0, 4.0]


def test_embed_texts_fails_fast_on_401_without_leaking_token():
    def post_401(url, headers=None, json=None, timeout=None):
        return _make_response(401, {"success": False, "errors": [{"code": 10000, "message": "Authentication error"}]})

    with pytest.raises(RuntimeError) as exc_info:
        ble.embed_texts(["hello"], account_id="acc", api_token="super-secret-token", post_fn=post_401)

    assert "401" in str(exc_info.value)
    assert "super-secret-token" not in str(exc_info.value)


def test_run_backfill_skips_unchanged_commits_per_batch_and_resumes_after_429(monkeypatch):
    lessons = [
        {
            "id": f"00000000-0000-0000-0000-00000000000{i}",
            "title": f"Lesson {i}",
            "program": None,
            "categories": [],
            "description": None,
            "transcript": f"Transcript text {i}",
        }
        for i in range(1, 6)
    ]

    first_content = ble.build_search_content(
        title="Lesson 1",
        program=None,
        categories=[],
        description=None,
        transcript="Transcript text 1",
    )
    existing_store: dict[str, str] = {
        lessons[0]["id"]: ble.compute_content_hash(ble.DEFAULT_MODEL, first_content)
    }
    commit_batches: list[list[str]] = []

    class FakeConn:
        def rollback(self):
            pass

    monkeypatch.setattr(ble, "fetch_lessons", lambda conn, include_all=False: lessons)
    monkeypatch.setattr(ble, "fetch_existing_hashes", lambda conn: dict(existing_store))

    def fake_upsert(conn, rows):
        batch_ids = [r["lesson_id"] for r in rows]
        commit_batches.append(batch_ids)
        for r in rows:
            existing_store[r["lesson_id"]] = r["content_hash"]

    monkeypatch.setattr(ble, "upsert_embeddings_batch", fake_upsert)

    calls: list[list[str]] = []

    def embed_fail_second_batch(texts):
        calls.append(texts)
        if len(calls) == 2:
            raise ble.RateLimitExhaustedError("Daily quota hit")
        return [[0.5] * ble.EXPECTED_DIMS for _ in texts]

    summary1 = ble.run_backfill(
        FakeConn(),
        embed_fail_second_batch,
        model=ble.DEFAULT_MODEL,
        batch_size=2,
        delay_ms=0,
        force=False,
        dry_run=False,
        limit=None,
        include_all=False,
        sleep_fn=lambda _: None,
    )

    # Lesson 1 skipped; lessons 2 & 3 committed; lessons 4 & 5 hit 429 and remain pending.
    assert summary1 == {
        "total": 5,
        "skipped": 1,
        "pending": 4,
        "upserted": 2,
        "remaining": 2,
        "rate_limited": 1,
        "interrupted": 0,
    }
    assert commit_batches == [[lessons[1]["id"], lessons[2]["id"]]]

    # Resume run: skips lessons 1, 2, 3 and embeds only 4 & 5.
    calls.clear()

    def embed_ok(texts):
        calls.append(texts)
        return [[0.5] * ble.EXPECTED_DIMS for _ in texts]

    summary2 = ble.run_backfill(
        FakeConn(),
        embed_ok,
        model=ble.DEFAULT_MODEL,
        batch_size=2,
        delay_ms=0,
        force=False,
        dry_run=False,
        limit=None,
        include_all=False,
        sleep_fn=lambda _: None,
    )

    assert summary2 == {
        "total": 5,
        "skipped": 3,
        "pending": 2,
        "upserted": 2,
        "remaining": 0,
        "rate_limited": 0,
        "interrupted": 0,
    }
    assert len(calls) == 1
    assert len(calls[0]) == 2


def test_run_backfill_handles_keyboard_interrupt_and_preserves_committed_batches(monkeypatch):
    lessons = [
        {
            "id": f"00000000-0000-0000-0000-00000000000{i}",
            "title": f"Lesson {i}",
            "program": None,
            "categories": [],
            "description": None,
            "transcript": f"Transcript text {i}",
        }
        for i in range(1, 5)
    ]
    rolled_back = {"count": 0}
    committed = []

    class FakeConn:
        def rollback(self):
            rolled_back["count"] += 1

    monkeypatch.setattr(ble, "fetch_lessons", lambda conn, include_all=False: lessons)
    monkeypatch.setattr(ble, "fetch_existing_hashes", lambda conn: {})
    monkeypatch.setattr(ble, "upsert_embeddings_batch", lambda conn, rows: committed.extend(r["lesson_id"] for r in rows))

    call_count = {"n": 0}

    def embed_interrupt_second_batch(texts):
        call_count["n"] += 1
        if call_count["n"] == 2:
            raise KeyboardInterrupt()
        return [[0.5] * ble.EXPECTED_DIMS for _ in texts]

    summary = ble.run_backfill(
        FakeConn(),
        embed_interrupt_second_batch,
        model=ble.DEFAULT_MODEL,
        batch_size=2,
        delay_ms=0,
        force=False,
        dry_run=False,
        limit=None,
        include_all=False,
        sleep_fn=lambda _: None,
    )

    assert rolled_back["count"] == 1
    assert committed == [lessons[0]["id"], lessons[1]["id"]]
    assert summary == {
        "total": 4,
        "skipped": 0,
        "pending": 4,
        "upserted": 2,
        "remaining": 2,
        "rate_limited": 0,
        "interrupted": 1,
    }
