import json
import os
import traceback

from app.groq_result import to_dict
from app.logging_config import logger
from app.media import download_file, download_youtube_audio_v2, extract_audio, is_youtube_url
from app.messaging import send_progress
from app.preprocessing import (
    build_speech_segments,
    build_time_mapping,
    create_processed_audio,
    detect_silence,
    get_audio_duration,
    remap_segments,
)
from app.segment_processing import build_canonical_segments
from app.transcription import transcribe_chunked_audio


def _extract_transcription_text_and_words(transcription_result):
    result_dict = to_dict(transcription_result)
    full_text = result_dict.get("text", "")
    words = result_dict.get("words", []) or []
    return full_text, [
        {
            "word": word.get("word", ""),
            "start": word.get("start", 0.0),
            "end": word.get("end", 0.0),
        }
        for word in words
    ]


def _prepare_audio(ch, lesson_id: str, file_url: str) -> tuple[str, str, str]:
    local_file_path = f"temp_{lesson_id}"
    audio_path = f"{local_file_path}.mp3"

    if is_youtube_url(file_url):
        send_progress(ch, lesson_id, 10, "Extracting YouTube audio...")
        download_youtube_audio_v2(file_url, audio_path)
    else:
        send_progress(ch, lesson_id, 10, "Downloading file...")
        download_file(file_url, local_file_path)
        send_progress(ch, lesson_id, 20, "Extracting audio...")
        extract_audio(local_file_path, audio_path)

    target_path = audio_path if os.path.exists(audio_path) else local_file_path
    return local_file_path, audio_path, target_path


def _remove_silence(ch, lesson_id: str, target_path: str) -> tuple[str, list | None, str | None]:
    send_progress(ch, lesson_id, 25, "Preprocessing audio (Silence removal)...")
    audio_duration = get_audio_duration(target_path)
    logger.info("Audio duration: %.2fs", audio_duration)

    silences = detect_silence(target_path)
    logger.info("SILENCE DETECTION RESULT: %s silence intervals found", len(silences))
    for i, silence in enumerate(silences):
        logger.info(
            "  Silence %s: [%.3fs, %.3fs] (%.3fs)",
            i,
            silence["start"],
            silence["end"],
            silence["end"] - silence["start"],
        )

    if not silences:
        logger.info("No silence detected. Skipping preprocessing step.")
        return target_path, None, None

    speech_segments = build_speech_segments(silences, audio_duration)
    logger.info("SPEECH SEGMENTS: %s segments", len(speech_segments))
    for i, segment in enumerate(speech_segments):
        logger.info(
            "  Speech %s: [%.3fs, %.3fs] (%.3fs)",
            i,
            segment["original_start"],
            segment["original_end"],
            segment["original_end"] - segment["original_start"],
        )

    if not speech_segments:
        raise Exception("Audio contains only silence, no speech detected.")

    processed_target_path = f"processed_{lesson_id}.mp3"
    create_processed_audio(target_path, speech_segments, processed_target_path)
    mapping = build_time_mapping(speech_segments)
    logger.info("TIME MAPPING created with %s segments", len(mapping))

    for i, item in enumerate(mapping):
        logger.info(
            "  Map %s: processed=[%.3f, %.3f]s -> original=[%.3f, %.3f]s",
            i,
            item["processed_start"],
            item["processed_end"],
            item["original_start"],
            item["original_end"],
        )

    return processed_target_path, mapping, processed_target_path


def _write_debug_json(lesson_id: str, suffix: str, payload: dict):
    path = f"debug_{lesson_id}_{suffix}.json"
    with open(path, "w", encoding="utf-8") as file:
        json.dump(payload, file, ensure_ascii=False, indent=2)
    logger.info("Saved %s JSON to %s", suffix.upper(), path)


def process_audio(ch, lesson_id: str, file_url: str) -> dict:
    local_file_path = f"temp_{lesson_id}"
    audio_path = f"{local_file_path}.mp3"
    processed_target_path = None

    try:
        local_file_path, audio_path, target_path = _prepare_audio(ch, lesson_id, file_url)
        audio_duration = get_audio_duration(target_path)
        final_audio_path, mapping, processed_target_path = _remove_silence(ch, lesson_id, target_path)

        send_progress(ch, lesson_id, 30, "Transcribing...")
        logger.info("Starting Whisper transcription for %s", lesson_id)
        raw_transcription_result, transcription_result = transcribe_chunked_audio(final_audio_path)
        _write_debug_json(lesson_id, "raw", raw_transcription_result)

        full_text, words_data = _extract_transcription_text_and_words(transcription_result)
        segments_data = build_canonical_segments(full_text, words_data)

        if mapping:
            remap_segments(segments_data, mapping)

        result_data = {"text": full_text.strip(), "segments": segments_data}
        _write_debug_json(lesson_id, "processed", result_data)

        return {"status": "COMPLETED", "result": result_data, "duration": audio_duration}

    except Exception as exc:
        logger.error("Error processing audio for %s:\n%s", lesson_id, traceback.format_exc())
        return {"status": "FAILED", "error": str(exc)}

    finally:
        for path in [local_file_path, audio_path, processed_target_path]:
            if path and os.path.exists(path):
                os.remove(path)
