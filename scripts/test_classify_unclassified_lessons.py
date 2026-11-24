#!/usr/bin/env python3
"""Unit tests for scripts/classify_unclassified_lessons.py."""

from __future__ import annotations

import sys
from pathlib import Path
from unittest.mock import MagicMock

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import classify_unclassified_lessons as cul
from backfill_lesson_embeddings import RateLimitExhaustedError


SAMPLE_CATEGORIES = [
    {"id": "cat-1", "name": "Technology", "name_vi": "Công nghệ", "slug": "technology", "description": "Công nghệ"},
    {"id": "cat-2", "name": "Science", "name_vi": "Khoa học", "slug": "science", "description": "Physics and biology"},
    {"id": "cat-3", "name": "Daily Life", "name_vi": "Đời sống hằng ngày", "slug": "daily-life", "description": "Đời sống"},
]


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


def test_build_lesson_classification_input_preserves_metadata_and_truncates():
    long_transcript = " ".join(f"word{i}" for i in range(2000))
    text = cul.build_lesson_classification_input(
        title="AI in Healthcare",
        program="BBC 6 Minute English",
        description="How AI helps doctors.",
        transcript=long_transcript,
        max_chars=250,
    )
    assert len(text) <= 250
    assert text.startswith(
        "Title: AI in Healthcare\n"
        "Program: BBC 6 Minute English\n"
        "Description: How AI helps doctors.\n"
        "Transcript: "
    )


def test_build_classification_messages_deduplicates_name_vi_and_description():
    messages = cul.build_classification_messages("Title: Demo", SAMPLE_CATEGORIES, max_categories=2)
    user_msg = messages[1]["content"]
    assert "- technology: Technology (Công nghệ)\n" in user_msg
    assert "- science: Science (Khoa học) - Physics and biology\n" in user_msg


def test_parse_classification_response_handles_dict_json_string_and_choices_fallback():
    valid = {"technology", "science", "daily-life"}

    payload_dict = {"success": True, "result": {"response": {"categories": ["technology", "technology", "invalid"]}}}
    assert cul.parse_classification_response(payload_dict, valid, max_categories=3) == ["technology"]

    payload_str = {
        "success": True,
        "result": {"response": '```json\n{"categories": ["science", "technology", "daily-life"]}\n```'},
    }
    assert cul.parse_classification_response(payload_str, valid, max_categories=2) == ["science", "technology"]

    payload_choices = {
        "success": True,
        "result": {"response": None, "choices": [{"message": {"content": '{"categories": ["daily-life"]}'}}]},
    }
    assert cul.parse_classification_response(payload_choices, valid, max_categories=3) == ["daily-life"]


def test_parse_classification_response_raises_on_no_valid_slugs():
    payload = {"success": True, "result": {"response": {"categories": ["unknown-slug"]}}}
    with pytest.raises(ValueError, match="No valid category slugs"):
        cul.parse_classification_response(payload, {"technology", "science"})


def test_classify_lesson_text_success_and_429_retry():
    calls = []

    def fake_post(url, headers=None, json=None, timeout=None):
        calls.append((url, json))
        if len(calls) == 1:
            return _make_response(429, payload={"errors": [{"code": 3036, "message": "Rate limit"}]}, headers={"Retry-After": "1"})
        return _make_response(200, payload={"success": True, "result": {"response": {"categories": ["technology", "science"]}}})

    sleeps = []
    slugs = cul.classify_lesson_text(
        "Title: Quantum Computing",
        SAMPLE_CATEGORIES,
        account_id="acc",
        api_token="tok",
        sleep_fn=sleeps.append,
        post_fn=fake_post,
    )
    assert slugs == ["technology", "science"]
    assert sleeps == [1.0]
    assert "@cf/meta/llama-3.3-70b-instruct-fp8-fast" in calls[0][0]
    assert calls[0][1]["response_format"]["type"] == "json_schema"


def test_classify_lesson_text_raises_fatal_on_401():
    def fake_post(url, headers=None, json=None, timeout=None):
        return _make_response(401, payload={"errors": [{"code": 10000, "message": "Unauthorized"}]})

    with pytest.raises(cul.FatalClassificationError, match="HTTP 401"):
        cul.classify_lesson_text(
            "Title: Demo",
            SAMPLE_CATEGORIES,
            account_id="acc",
            api_token="bad-token",
            post_fn=fake_post,
        )


def test_classify_lesson_text_raises_rate_limit_exhausted_on_long_retry_after():
    def fake_post(url, headers=None, json=None, timeout=None):
        return _make_response(
            429,
            payload={"errors": [{"code": 3036, "message": "Daily quota exceeded"}]},
            headers={"Retry-After": "3600"},
        )

    with pytest.raises(RateLimitExhaustedError):
        cul.classify_lesson_text(
            "Title: Demo",
            SAMPLE_CATEGORIES,
            account_id="acc",
            api_token="tok",
            post_fn=fake_post,
        )


def test_run_classification_saves_per_lesson_and_handles_rate_limit(monkeypatch):
    monkeypatch.setattr(cul, "fetch_active_categories", lambda conn: SAMPLE_CATEGORIES)
    monkeypatch.setattr(
        cul,
        "fetch_unclassified_lessons",
        lambda conn, include_all=False, force=False: [
            {"id": "l-1", "title": "Lesson 1", "program": None, "description": None, "transcript": "About AI"},
            {"id": "l-2", "title": "Lesson 2", "program": None, "description": None, "transcript": "About physics"},
        ],
    )

    saved = []
    monkeypatch.setattr(
        cul,
        "save_lesson_categories",
        lambda conn, lesson_id, category_ids, replace=False: saved.append((lesson_id, list(category_ids), replace)),
    )

    conn = MagicMock()
    call_count = 0

    def fake_classify(text, categories):
        nonlocal call_count
        call_count += 1
        if call_count == 1:
            return ["technology"]
        raise RateLimitExhaustedError("Daily quota reached")

    summary = cul.run_classification(conn, fake_classify, delay_ms=0)
    assert summary["classified"] == 1
    assert summary["remaining"] == 1
    assert summary["rate_limited"] == 1
    assert saved == [("l-1", ["cat-1"], False)]
    conn.rollback.assert_called_once()


def test_run_classification_fail_fast_on_fatal_error(monkeypatch):
    monkeypatch.setattr(cul, "fetch_active_categories", lambda conn: SAMPLE_CATEGORIES)
    monkeypatch.setattr(
        cul,
        "fetch_unclassified_lessons",
        lambda conn, include_all=False, force=False: [
            {"id": "l-1", "title": "Lesson 1", "program": None, "description": None, "transcript": "About AI"},
            {"id": "l-2", "title": "Lesson 2", "program": None, "description": None, "transcript": "About physics"},
        ],
    )

    conn = MagicMock()
    calls = 0

    def fake_classify(text, categories):
        nonlocal calls
        calls += 1
        raise cul.FatalClassificationError("HTTP 401: Unauthorized")

    with pytest.raises(cul.FatalClassificationError):
        cul.run_classification(conn, fake_classify, delay_ms=0)

    assert calls == 1
    conn.rollback.assert_called_once()


def test_run_classification_dry_run_and_exclude_slugs(monkeypatch, capsys):
    monkeypatch.setattr(cul, "fetch_active_categories", lambda conn: SAMPLE_CATEGORIES)
    monkeypatch.setattr(
        cul,
        "fetch_unclassified_lessons",
        lambda conn, include_all=False, force=False: [
            {"id": "l-1", "title": "Lesson 1", "program": None, "description": None, "transcript": "About AI"},
        ],
    )

    conn = MagicMock()
    called = False

    def fake_classify(text, categories):
        nonlocal called
        called = True
        return ["technology"]

    summary = cul.run_classification(
        conn,
        fake_classify,
        dry_run=True,
        exclude_slugs=["daily-life"],
    )
    out = capsys.readouterr().out
    assert not called
    assert summary["pending"] == 1
    assert "Active categories (2):" in out
    assert "daily-life" not in out


def test_run_classification_force_replaces_and_handles_keyboard_interrupt(monkeypatch):
    monkeypatch.setattr(cul, "fetch_active_categories", lambda conn: SAMPLE_CATEGORIES)
    monkeypatch.setattr(
        cul,
        "fetch_unclassified_lessons",
        lambda conn, include_all=False, force=False: [
            {"id": "l-1", "title": "Lesson 1", "program": None, "description": None, "transcript": "About AI"},
            {"id": "l-2", "title": "Lesson 2", "program": None, "description": None, "transcript": "About physics"},
        ],
    )

    saved = []
    monkeypatch.setattr(
        cul,
        "save_lesson_categories",
        lambda conn, lesson_id, category_ids, replace=False: saved.append((lesson_id, list(category_ids), replace)),
    )

    conn = MagicMock()
    call_count = 0

    def fake_classify(text, categories):
        nonlocal call_count
        call_count += 1
        if call_count == 1:
            return ["science"]
        raise KeyboardInterrupt()

    summary = cul.run_classification(conn, fake_classify, delay_ms=0, force=True)
    assert summary["classified"] == 1
    assert summary["interrupted"] == 1
    assert saved == [("l-1", ["cat-2"], True)]
    conn.rollback.assert_called_once()
