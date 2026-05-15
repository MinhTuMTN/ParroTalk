import json
import os

from app.logging_config import logger


MAX_GAP_SECONDS = 0.5
MIN_WORDS_PER_SEGMENT = 8
MAX_WORDS_PER_SEGMENT = 25
TERMINAL_PUNCTUATIONS = {".", "?", "!"}

MAX_FILE_SIZE_MB = 25
AUDIO_CHUNK_DURATION = 600

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")

PROCESSING_QUEUE = "audio-processing-queue"
RESULT_QUEUE = "audio-result-queue"

RAPIDAPI_HOST = "youtube-mp3-audio-video-downloader.p.rapidapi.com"


def _load_keys(primary_name: str, fallback_name: str) -> list[str]:
    raw = (os.getenv(primary_name) or os.getenv(fallback_name) or "").strip()
    if not raw:
        raise ValueError(f"Missing {primary_name} (or {fallback_name}) environment variable")

    if raw.startswith("["):
        try:
            keys = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ValueError(f"{primary_name} must be valid JSON when using array format") from exc
    else:
        keys = [key.strip() for key in raw.split(",") if key.strip()]

    if not isinstance(keys, list) or not all(isinstance(key, str) and key.strip() for key in keys):
        raise ValueError(f"{primary_name} must contain one or more non-empty strings")

    return [key.strip() for key in keys]


def load_groq_keys() -> list[str]:
    return _load_keys("GROQ_API_KEYS", "GROQ_API_KEY")


def load_rapidapi_keys() -> list[str]:
    return _load_keys("RAPIDAPI_KEYS", "RAPIDAPI_KEY")


# Preserve the current fallback key behavior, while allowing env-based rotation in deployments.
try:
    RAPIDAPI_KEYS = load_rapidapi_keys()
except ValueError:
    RAPIDAPI_KEYS = ["9a01cdd41emsh10cc37e0eaf3c36p17788bjsn393b05a5b8bb"]

logger.info("Loaded %s RapidAPI key(s) for rotation.", len(RAPIDAPI_KEYS))
