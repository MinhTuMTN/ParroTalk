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
TTS_SYNTHESIS_QUEUE = os.getenv("TTS_SYNTHESIS_QUEUE", "tts-synthesis-queue")
TTS_RESULT_QUEUE = os.getenv("TTS_RESULT_QUEUE", "tts-result-queue")
TTS_DLQ = os.getenv("TTS_DLQ", "tts-synthesis-dlq")
TTS_CONCURRENCY = max(1, int(os.getenv("TTS_CONCURRENCY", "4")))

CLOUDINARY_CLOUD_NAME = os.getenv("CLOUDINARY_CLOUD_NAME", "")
CLOUDINARY_API_KEY = os.getenv("CLOUDINARY_API_KEY", "")
CLOUDINARY_API_SECRET = os.getenv("CLOUDINARY_API_SECRET", "")
CLOUDINARY_TTS_FOLDER = os.getenv("CLOUDINARY_TTS_FOLDER", "tts")

RAPIDAPI_HOST = "youtube-mp3-audio-video-downloader.p.rapidapi.com"

TTS_VOICE_US = os.getenv("TTS_VOICE_US", "en-US-JennyNeural")
TTS_VOICE_UK = os.getenv("TTS_VOICE_UK", "en-GB-SoniaNeural")
TTS_ALLOWED_VOICES_US = ("en-US-JennyNeural", "en-US-GuyNeural", "en-US-AriaNeural")
TTS_ALLOWED_VOICES_UK = ("en-GB-SoniaNeural", "en-GB-RyanNeural", "en-GB-LibbyNeural")
TTS_ALLOWED_VOICES = TTS_ALLOWED_VOICES_US + TTS_ALLOWED_VOICES_UK
TTS_VOICE_LOOKUP_BY_ACCENT = {
    "US": {voice.lower(): voice for voice in TTS_ALLOWED_VOICES_US},
    "UK": {voice.lower(): voice for voice in TTS_ALLOWED_VOICES_UK},
}
TTS_VOICE_LOOKUP = {
    **TTS_VOICE_LOOKUP_BY_ACCENT["US"],
    **TTS_VOICE_LOOKUP_BY_ACCENT["UK"],
}
TTS_MAX_TEXT_LENGTH = int(os.getenv("TTS_MAX_TEXT_LENGTH", "500"))
TTS_CACHE_DIR = os.getenv("TTS_CACHE_DIR", "tts_cache")


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
