#!/usr/bin/env python3
"""Check a comma-separated list of Groq API keys against the Groq Chat Completions API."""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
DEFAULT_MODEL = "qwen/qwen3.8-27b"


def mask_key(key: str) -> str:
    if len(key) <= 10:
        return "***"
    return f"{key[:4]}...{key[-4:]}"


def parse_keys(raw: str) -> list[str]:
    text = raw.strip()
    if not text:
        return []
    if text.startswith("["):
        try:
            items = json.loads(text)
            if isinstance(items, list):
                return [str(x).strip().strip('"').strip("'") for x in items if str(x).strip()]
        except json.JSONDecodeError:
            pass
    parts = [p.strip().strip('"').strip("'") for p in text.split(",")]
    seen: set[str] = set()
    unique: list[str] = []
    for p in parts:
        if p and p not in seen:
            seen.add(p)
            unique.append(p)
    return unique


def load_keys_from_env(env_path: Path) -> list[str]:
    for line in env_path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        k, v = stripped.split("=", 1)
        if k.strip() == "GROQ_API_KEYS":
            return parse_keys(v)
    return []


def check_key(key: str, model: str, timeout: float) -> tuple[str, int | None, str]:
    payload = json.dumps(
        {
            "model": model,
            "messages": [{"role": "user", "content": "hi"}],
            "max_tokens": 1,
            "temperature": 0,
        }
    ).encode("utf-8")

    req = urllib.request.Request(
        GROQ_CHAT_URL,
        data=payload,
        method="POST",
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
            "User-Agent": "parrotalk-groq-key-checker/1.0",
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            rem_req = resp.headers.get("x-ratelimit-remaining-requests", "?")
            rem_tok = resp.headers.get("x-ratelimit-remaining-tokens", "?")
            return "OK", resp.status, f"rem_req={rem_req}, rem_tok={rem_tok}"
    except urllib.error.HTTPError as err:
        body = err.read().decode("utf-8", errors="replace")
        msg = body
        try:
            msg = json.loads(body).get("error", {}).get("message", body)
        except json.JSONDecodeError:
            pass

        lower = msg.lower()
        if err.code == 401:
            return "INVALID", err.code, f"[DEAD - key sai/da thu hoi] {msg}"
        if err.code == 429:
            retry_after = err.headers.get("retry-after")
            wait_info = f"retry_after={retry_after}s" if retry_after else "temporary rate limit"
            return "RATE_LIMITED", err.code, f"[TEMP - {wait_info}] {msg}"
        if "restricted" in lower or "organization" in lower:
            return "RESTRICTED", err.code, f"[PERM - org bi khoa, hay xoa khoi .env] {msg}"
        return "ERROR", err.code, msg
    except Exception as exc:
        return "ERROR", None, str(exc)


def copy_to_clipboard(text: str) -> bool:
    if sys.platform.startswith("win"):
        try:
            subprocess.run(["clip"], input=text.encode("utf-8"), check=True)
            return True
        except Exception:
            return False

    candidates = [
        ["pbcopy"],
        ["wl-copy"],
        ["xclip", "-selection", "clipboard"],
        ["xsel", "--clipboard", "--input"],
    ]
    for cmd in candidates:
        if shutil.which(cmd[0]):
            try:
                subprocess.run(cmd, input=text.encode("utf-8"), check=True)
                return True
            except Exception:
                continue
    return False


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Check comma-separated Groq API keys against Groq Chat Completions API."
    )
    parser.add_argument(
        "keys",
        nargs="?",
        help="Comma-separated Groq API keys (e.g. gsk_a,gsk_b). Reads stdin/prompt if omitted.",
    )
    parser.add_argument("--file", type=Path, help="Read comma-separated keys from a file.")
    parser.add_argument("--env", type=Path, help="Read GROQ_API_KEYS from a .env file.")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"Model to test (default: {DEFAULT_MODEL}).")
    parser.add_argument("--timeout", type=float, default=15.0, help="HTTP timeout in seconds (default: 15).")
    parser.add_argument(
        "--strict-ok",
        action="store_true",
        help="Only keep HTTP 200 OK keys (exclude temporarily 429 RATE_LIMITED keys).",
    )
    parser.add_argument(
        "--no-clipboard",
        action="store_true",
        help="Do not copy usable keys to the system clipboard.",
    )
    parser.add_argument(
        "--print-valid",
        action="store_true",
        help="Print comma-separated usable keys at the end (masked unless --raw is also passed).",
    )
    parser.add_argument(
        "--raw",
        action="store_true",
        help="Show full unmasked usable keys when used with --print-valid.",
    )
    args = parser.parse_args()

    keys: list[str] = []
    if args.env:
        keys = load_keys_from_env(args.env)
    elif args.file:
        keys = parse_keys(args.file.read_text(encoding="utf-8"))
    elif args.keys:
        keys = parse_keys(args.keys)
    elif not sys.stdin.isatty():
        keys = parse_keys(sys.stdin.read())
    else:
        raw_input = input("Nhap danh sach Groq API keys (cach nhau boi dau phay): ")
        keys = parse_keys(raw_input)

    if not keys:
        print("Khong tim thay API key nao de kiem tra.", file=sys.stderr)
        return 1

    print(f"Checking {len(keys)} Groq API key(s) with model '{args.model}'...\n")
    usable_statuses = {"OK"} if args.strict_ok else {"OK", "RATE_LIMITED"}
    usable_keys: list[str] = []
    counts: dict[str, int] = {}

    for idx, key in enumerate(keys, start=1):
        status, code, detail = check_key(key, args.model, args.timeout)
        counts[status] = counts.get(status, 0) + 1
        if status in usable_statuses:
            usable_keys.append(key)
        code_str = str(code) if code is not None else "-"
        short_detail = detail if len(detail) <= 120 else detail[:117] + "..."
        print(f"[{idx:02d}/{len(keys):02d}] {mask_key(key):<14} | {status:<12} | HTTP {code_str:<4} | {short_detail}")

    print("\nSummary:", ", ".join(f"{k}={v}" for k, v in sorted(counts.items())))

    if not args.no_clipboard:
        clipboard_text = ",".join(usable_keys)
        if copy_to_clipboard(clipboard_text):
            print(f"\nCopied {len(usable_keys)} usable key(s) to clipboard (comma-separated).")
        else:
            print("\nWarning: Could not access system clipboard.", file=sys.stderr)

    if args.print_valid and usable_keys:
        out = usable_keys if args.raw else [mask_key(k) for k in usable_keys]
        print("\nUsable keys (comma-separated):")
        print(",".join(out))

    return 0 if usable_keys else 2


if __name__ == "__main__":
    raise SystemExit(main())
