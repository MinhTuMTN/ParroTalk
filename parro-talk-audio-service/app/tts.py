"""Text-to-speech synthesis helpers powered by Edge TTS."""

from __future__ import annotations

import argparse
from collections.abc import Mapping, Sequence
import asyncio
import hashlib
import io
import os
from pathlib import Path
import re
import sys
import tempfile

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import cloudinary
import cloudinary.uploader
import edge_tts

from app.config import (
    CLOUDINARY_API_KEY,
    CLOUDINARY_API_SECRET,
    CLOUDINARY_CLOUD_NAME,
    CLOUDINARY_TTS_FOLDER,
    TTS_CONCURRENCY,
    TTS_MAX_TEXT_LENGTH,
    TTS_VOICE_LOOKUP_BY_ACCENT,
    TTS_VOICE_UK,
    TTS_VOICE_US,
)
from app.logging_config import logger


_UK_ACCENTS = frozenset({"UK", "GB", "EN-GB", "EN_GB"})
_CONTROL_CHARS_RE = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
_VALUE_FLAG_OPTIONS = frozenset({"--text", "--voice"})


def normalize_accent(accent: str | None = "US") -> str:
    """Normalize an accent identifier to 'UK' or 'US' (default 'US')."""
    if isinstance(accent, str) and accent.strip().upper() in _UK_ACCENTS:
        return "UK"
    return "US"


def resolve_voice(accent: str | None = "US", voice: str | None = None) -> str:
    """Resolve an accent-matching allowlisted Edge TTS voice or fall back to default."""
    norm_accent = normalize_accent(accent)
    if isinstance(voice, str):
        canonical = TTS_VOICE_LOOKUP_BY_ACCENT[norm_accent].get(voice.strip().lower())
        if canonical is not None:
            return canonical
    return TTS_VOICE_UK if norm_accent == "UK" else TTS_VOICE_US


def validate_and_clean_text(text: str) -> str:
    """Strip control characters, collapse whitespace, and enforce text limits."""
    if not isinstance(text, str):
        raise ValueError("Text must be a non-empty string.")

    without_controls = _CONTROL_CHARS_RE.sub("", text)
    cleaned = " ".join(without_controls.split())
    if not cleaned:
        raise ValueError("Text must not be empty.")

    if len(cleaned) > TTS_MAX_TEXT_LENGTH:
        raise ValueError(
            f"Text length ({len(cleaned)}) exceeds maximum of {TTS_MAX_TEXT_LENGTH} characters."
        )

    return cleaned


def compute_tts_cache_key(
    text: str,
    accent: str | None = "US",
    voice: str | None = None,
) -> str:
    """Compute a deterministic cache key from normalized accent, voice, and cleaned text."""
    cleaned = validate_and_clean_text(text)
    norm_accent = normalize_accent(accent)
    resolved_voice = resolve_voice(norm_accent, voice)
    default_voice = resolve_voice(norm_accent, None)
    hash_input = (
        f"{norm_accent}:{cleaned}"
        if resolved_voice == default_voice
        else f"{norm_accent}:{resolved_voice}:{cleaned}"
    )
    digest = hashlib.sha256(hash_input.encode("utf-8")).hexdigest()
    return f"{norm_accent.lower()}_{digest}"


async def synthesize_speech_bytes_async(
    text: str,
    accent: str | None = "US",
    voice: str | None = None,
) -> bytes:
    """Asynchronously synthesize MP3 speech bytes via Edge TTS."""
    cleaned = validate_and_clean_text(text)
    resolved_voice = resolve_voice(accent, voice)
    communicate = edge_tts.Communicate(cleaned, resolved_voice)
    audio_buffer = bytearray()

    async for chunk in communicate.stream():
        if isinstance(chunk, Mapping) and chunk.get("type") == "audio":
            data = chunk.get("data")
            if isinstance(data, bytes) and data:
                audio_buffer.extend(data)

    if not audio_buffer:
        raise RuntimeError(
            f"Edge TTS returned empty audio stream for voice '{resolved_voice}'."
        )

    return bytes(audio_buffer)


def synthesize_speech_bytes(
    text: str,
    accent: str | None = "US",
    voice: str | None = None,
) -> bytes:
    """Synchronously synthesize MP3 speech bytes via Edge TTS."""
    return asyncio.run(synthesize_speech_bytes_async(text, accent, voice))


def synthesize_speech_file(
    text: str,
    output_path: str | Path,
    accent: str | None = "US",
    voice: str | None = None,
) -> Path:
    """Synthesize speech to output_path atomically, reusing non-empty cached files."""
    cleaned = validate_and_clean_text(text)
    target_path = Path(output_path)
    target_path.parent.mkdir(parents=True, exist_ok=True)

    if target_path.is_file() and target_path.stat().st_size > 0:
        logger.info("Using cached TTS audio file: %s", target_path)
        return target_path

    audio_bytes = synthesize_speech_bytes(cleaned, accent, voice)
    tmp_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=target_path.parent,
            prefix=f".{target_path.stem or 'tts'}.",
            suffix=".tmp",
            delete=False,
        ) as tmp_file:
            tmp_path = Path(tmp_file.name)
            tmp_file.write(audio_bytes)
            tmp_file.flush()
            os.fsync(tmp_file.fileno())
        os.replace(tmp_path, target_path)
    except Exception:
        if tmp_path is not None:
            tmp_path.unlink(missing_ok=True)
        raise

    logger.info("Synthesized TTS audio file: %s", target_path)
    return target_path


def _configure_cloudinary() -> None:
    """Configure Cloudinary SDK from explicit env vars or CLOUDINARY_URL."""
    if CLOUDINARY_CLOUD_NAME and CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET:
        cloudinary.config(
            cloud_name=CLOUDINARY_CLOUD_NAME,
            api_key=CLOUDINARY_API_KEY,
            api_secret=CLOUDINARY_API_SECRET,
            secure=True,
        )
    elif os.getenv("CLOUDINARY_URL"):
        cloudinary.config(secure=True)
    else:
        raise RuntimeError("Missing Cloudinary credentials (CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET).")


def upload_tts_audio_to_cloudinary(audio_bytes: bytes, cache_key: str) -> str:
    """Upload synthesized MP3 bytes to Cloudinary using a deterministic public_id."""
    if not audio_bytes:
        raise ValueError("audio_bytes must not be empty.")
    _configure_cloudinary()
    folder = (CLOUDINARY_TTS_FOLDER or "tts").strip("/")
    public_id = f"{folder}/{cache_key}" if folder else cache_key
    response = cloudinary.uploader.upload(
        io.BytesIO(audio_bytes),
        resource_type="video",
        public_id=public_id,
        format="mp3",
        overwrite=False,
        unique_filename=False,
    )
    secure_url = response.get("secure_url") or response.get("url")
    if not secure_url:
        raise RuntimeError(f"Cloudinary upload returned no URL for public_id={public_id}")
    return str(secure_url)


async def synthesize_and_upload_batch_async(
    items: Sequence[Mapping[str, object]],
    concurrency: int = TTS_CONCURRENCY,
) -> list[dict[str, str]]:
    """Synthesize and upload a batch of TTS items concurrently with bounded asyncio semaphore."""
    semaphore = asyncio.Semaphore(max(1, concurrency))

    async def process_one(raw_item: Mapping[str, object]) -> dict[str, str] | None:
        raw_text = raw_item.get("text")
        if not isinstance(raw_text, str):
            return None
        accent = raw_item.get("accent")
        accent_str = str(accent) if accent is not None else "US"
        voice = raw_item.get("voice")
        voice_str = str(voice) if voice is not None else None
        explicit_key = raw_item.get("cacheKey")

        try:
            cleaned = validate_and_clean_text(raw_text)
            norm_accent = normalize_accent(accent_str)
            resolved_voice = resolve_voice(norm_accent, voice_str)
            cache_key = (
                str(explicit_key).strip()
                if isinstance(explicit_key, str) and explicit_key.strip()
                else compute_tts_cache_key(cleaned, norm_accent, resolved_voice)
            )
        except Exception as exc:
            logger.warning("Skipping invalid TTS batch item: %s", exc)
            return None

        async with semaphore:
            try:
                audio_bytes = await synthesize_speech_bytes_async(cleaned, norm_accent, resolved_voice)
                cloudinary_url = await asyncio.to_thread(
                    upload_tts_audio_to_cloudinary,
                    audio_bytes,
                    cache_key,
                )
                return {
                    "cacheKey": cache_key,
                    "cloudinaryUrl": cloudinary_url,
                    "accent": norm_accent,
                    "voice": resolved_voice,
                }
            except Exception as exc:
                logger.warning("TTS batch item failed for key=%s: %s", cache_key, exc)
                return None

    results = await asyncio.gather(*(process_one(item) for item in items))
    return [res for res in results if res is not None]


def synthesize_and_upload_batch(
    items: Sequence[Mapping[str, object]],
    concurrency: int = TTS_CONCURRENCY,
) -> list[dict[str, str]]:
    """Synchronously synthesize and upload a batch of TTS items to Cloudinary."""
    if not items:
        return []
    return asyncio.run(synthesize_and_upload_batch_async(items, concurrency=concurrency))


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Synthesize English speech audio using Edge TTS."
    )
    parser.add_argument("--text", required=True, help="Text to synthesize.")
    parser.add_argument(
        "--accent",
        default="US",
        help="Speech accent (e.g. US or UK). Defaults to US.",
    )
    parser.add_argument(
        "--voice",
        default=None,
        help="Optional Edge TTS voice override (e.g. en-US-GuyNeural).",
    )
    parser.add_argument(
        "--output",
        required=True,
        help="Output audio file path.",
    )
    return parser


def _normalize_cli_args(argv: Sequence[str] | None) -> list[str]:
    """Attach `--text`/`--voice <value>` as `--flag=<value>` so leading hyphens stay values."""
    raw_args = list(sys.argv[1:] if argv is None else argv)
    normalized: list[str] = []
    idx = 0
    while idx < len(raw_args):
        token = raw_args[idx]
        if token == "--":
            normalized.extend(raw_args[idx:])
            break
        if token in _VALUE_FLAG_OPTIONS and idx + 1 < len(raw_args):
            normalized.append(f"{token}={raw_args[idx + 1]}")
            idx += 2
            continue
        normalized.append(token)
        idx += 1
    return normalized


def main(argv: Sequence[str] | None = None) -> int:
    """CLI entrypoint for synthesizing speech audio to a file."""
    parser = _build_arg_parser()
    args = parser.parse_args(_normalize_cli_args(argv))
    try:
        output_path = synthesize_speech_file(
            text=args.text,
            output_path=args.output,
            accent=args.accent,
            voice=args.voice,
        )
    except Exception as exc:
        print(f"TTS synthesis failed: {exc}", file=sys.stderr)
        return 1

    print(str(output_path))
    return 0


if __name__ == "__main__":
    sys.exit(main())
