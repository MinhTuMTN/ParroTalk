import itertools
import os

from groq import Groq
from tenacity import retry, stop_after_attempt, wait_exponential

from app.config import (
    AUDIO_CHUNK_DURATION,
    MAX_FILE_SIZE_MB,
    load_groq_keys,
)
from app.chunk_processing import merge_chunk_results
from app.groq_result import to_dict
from app.logging_config import logger
from app.preprocessing import extract_audio_chunk, get_audio_duration


GROQ_API_KEYS = load_groq_keys()
GROQ_CLIENTS = [Groq(api_key=key) for key in GROQ_API_KEYS]
GROQ_KEY_MASKS = [f"...{key[-6:]}" if len(key) >= 6 else "...***" for key in GROQ_API_KEYS]
GROQ_CLIENT_CYCLE = itertools.cycle(list(zip(GROQ_CLIENTS, GROQ_KEY_MASKS)))
logger.info("Loaded %s Groq key(s) for rotation.", len(GROQ_API_KEYS))


@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10),
    reraise=True,
)
def transcribe_with_groq(file_path: str):
    logger.info("Transcribing %s with Groq Whisper...", file_path)

    last_error = None
    for attempt in range(len(GROQ_CLIENTS)):
        client, key_mask = next(GROQ_CLIENT_CYCLE)

        with open(file_path, "rb") as file:
            try:
                transcription = client.audio.transcriptions.create(
                    file=file,
                    model="whisper-large-v3",
                    response_format="verbose_json",
                    timestamp_granularities=["word", "segment"],
                    language="en",
                    temperature=0.0,
                    timeout=300,
                )
                logger.info(
                    "Transcription completed successfully with key %s. Duration: %ss",
                    key_mask,
                    transcription.duration,
                )
                return transcription
            except Exception as exc:
                last_error = exc
                logger.warning(
                    "Groq transcription failed on key %s (%s/%s): %s",
                    key_mask,
                    attempt + 1,
                    len(GROQ_CLIENTS),
                    exc,
                )

    logger.error("All Groq keys failed for this attempt: %s", last_error)
    raise last_error


def should_chunk_audio(file_size_bytes):
    max_size = MAX_FILE_SIZE_MB * 1024 * 1024
    return file_size_bytes > max_size


def transcribe_chunked_audio(audio_path: str):
    file_size = os.path.getsize(audio_path)

    if not should_chunk_audio(file_size):
        logger.info("File size %s is under limit, transcribing normally...", file_size)
        transcription = transcribe_with_groq(audio_path)
        transcription_dict = to_dict(transcription)
        return transcription_dict, transcription_dict

    logger.warning("File size %s exceeds %sMB, using chunking strategy...", file_size, MAX_FILE_SIZE_MB)
    audio_duration = get_audio_duration(audio_path)
    logger.info("Total audio duration: %ss", audio_duration)

    raw_results = []
    merge_inputs = []
    num_chunks = int(audio_duration / AUDIO_CHUNK_DURATION) + (1 if audio_duration % AUDIO_CHUNK_DURATION else 0)
    logger.info("Will split into %s chunks of %ss each", num_chunks, AUDIO_CHUNK_DURATION)

    for chunk_index, chunk_start in enumerate(range(0, int(audio_duration), AUDIO_CHUNK_DURATION)):
        chunk_end = min(chunk_start + AUDIO_CHUNK_DURATION, audio_duration)
        chunk_duration = chunk_end - chunk_start
        chunk_path = f"{audio_path}.chunk_{chunk_index}.mp3"

        try:
            logger.info("Extracting chunk %s: %ss - %ss", chunk_index, chunk_start, chunk_end)
            extract_audio_chunk(audio_path, chunk_path, chunk_start, int(chunk_duration))

            logger.info("Transcribing chunk %s...", chunk_index)
            result = transcribe_with_groq(chunk_path)
            result_dict = to_dict(result)
            raw_results.append(result_dict)
            merge_inputs.append((result_dict, chunk_start))

            logger.info("Chunk %s completed: %s segments", chunk_index, len(result.segments))
        except Exception as exc:
            logger.error("Error processing chunk %s: %s", chunk_index, exc)
            raise
        finally:
            if os.path.exists(chunk_path):
                os.remove(chunk_path)

    merged_result = merge_chunk_results(merge_inputs)
    logger.info("Chunked transcription completed: %s total segments", len(merged_result["segments"]))
    return raw_results, merged_result
