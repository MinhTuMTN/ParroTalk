import argparse
import json
import logging
import math
import os
import re
import subprocess
import tempfile
import time
from pathlib import Path

from groq import Groq


LOGGER = logging.getLogger("AudioDebug")
TERMINAL_RE = re.compile(r"[.!?][\"')\]]*$")
SOFT_RE = re.compile(r"[,;:][\"')\]]*$")
BREAK_WORDS = {
    "and",
    "or",
    "but",
    "so",
    "because",
    "although",
    "though",
    "while",
    "when",
    "which",
    "that",
    "then",
    "however",
    "therefore",
}


def configure_logging(verbose: bool):
    logging.basicConfig(
        level=logging.DEBUG if verbose else logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )


def load_api_keys() -> list[str]:
    raw = (os.getenv("GROQ_API_KEYS") or os.getenv("GROQ_API_KEY") or "").strip()
    if not raw:
        raise ValueError("Missing GROQ_API_KEYS or GROQ_API_KEY environment variable")

    if raw.startswith("["):
        keys = json.loads(raw)
    else:
        keys = [item.strip() for item in raw.split(",") if item.strip()]

    if not keys or not all(isinstance(key, str) and key.strip() for key in keys):
        raise ValueError("GROQ_API_KEYS must contain at least one non-empty key")

    return keys


def mask_key(key: str) -> str:
    return f"...{key[-6:]}" if len(key) >= 6 else "...***"


def to_dict(obj) -> dict:
    if isinstance(obj, dict):
        return obj
    if hasattr(obj, "model_dump"):
        return obj.model_dump()
    if hasattr(obj, "dict"):
        return obj.dict()
    if hasattr(obj, "__dict__"):
        return obj.__dict__
    return {}


def run_command(args: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(args, capture_output=True, text=True, check=True)


def get_audio_duration(audio_path: Path) -> float:
    result = run_command([
        "ffprobe",
        "-v",
        "error",
        "-show_entries",
        "format=duration",
        "-of",
        "default=noprint_wrappers=1:nokey=1",
        str(audio_path),
    ])
    return float(result.stdout.strip())


def extract_chunk(audio_path: Path, chunk_path: Path, start_time: int, duration: int):
    subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-i",
            str(audio_path),
            "-ss",
            str(start_time),
            "-t",
            str(duration),
            "-acodec",
            "libmp3lame",
            "-q:a",
            "2",
            str(chunk_path),
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=True,
    )


def transcribe_with_rotation(audio_path: Path, keys: list[str], model: str, language: str):
    last_error = None
    for key in keys:
        client = Groq(api_key=key)
        key_mask = mask_key(key)
        LOGGER.info("Trying Groq key %s", key_mask)

        try:
            with audio_path.open("rb") as file:
                result = client.audio.transcriptions.create(
                    file=file,
                    model=model,
                    response_format="verbose_json",
                    timestamp_granularities=["word", "segment"],
                    language=language,
                    temperature=0.0,
                    timeout=300,
                )
            LOGGER.info("Groq transcription succeeded with key %s", key_mask)
            return result
        except Exception as exc:
            last_error = exc
            LOGGER.warning("Groq key %s failed: %s", key_mask, exc)
            time.sleep(1)

    raise RuntimeError(f"All Groq keys failed. Last error: {last_error}")


def normalize_words(raw_words) -> list[dict]:
    words = []
    for raw_word in raw_words or []:
        word_data = to_dict(raw_word)
        word_text = word_data.get("word", "")
        start = word_data.get("start")
        end = word_data.get("end")
        if word_text and start is not None and end is not None:
            words.append({
                "word": str(word_text).strip(),
                "start": float(start),
                "end": float(end),
            })
    return words


def normalize_segments(raw_segments) -> list[dict]:
    segments = []
    for raw_segment in raw_segments or []:
        segment_data = to_dict(raw_segment)
        segments.append({
            "start": float(segment_data.get("start", 0.0)),
            "end": float(segment_data.get("end", 0.0)),
            "text": str(segment_data.get("text", "")).strip(),
            "words": normalize_words(segment_data.get("words", [])),
        })
    return segments


def collect_word_level_result(transcription_result) -> tuple[str, list[dict], list[dict]]:
    result_data = to_dict(transcription_result)
    full_text = str(result_data.get("text") or getattr(transcription_result, "text", "")).strip()
    raw_segments = normalize_segments(result_data.get("segments") or getattr(transcription_result, "segments", []))
    words = normalize_words(result_data.get("words") or getattr(transcription_result, "words", []))

    if not words:
        for segment in raw_segments:
            words.extend(segment.get("words", []))

    if not words:
        raise RuntimeError(
            "Groq response did not include word-level timestamps. "
            "Debug cannot continue because dictation accuracy depends on word-level timing."
        )

    return full_text, raw_segments, words


def adjust_timestamps(words: list[dict], offset: float) -> list[dict]:
    return [
        {
            "word": word["word"],
            "start": word["start"] + offset,
            "end": word["end"] + offset,
        }
        for word in words
    ]


def transcribe_audio(audio_path: Path, keys: list[str], model: str, language: str, chunk_seconds: int):
    file_size_mb = audio_path.stat().st_size / (1024 * 1024)
    if file_size_mb <= 24:
        result = transcribe_with_rotation(audio_path, keys, model, language)
        return collect_word_level_result(result)

    duration = get_audio_duration(audio_path)
    chunks = math.ceil(duration / chunk_seconds)
    LOGGER.info("Audio is %.2fMB and %.2fs. Splitting into %s chunks.", file_size_mb, duration, chunks)

    full_text_parts = []
    raw_segments = []
    all_words = []

    with tempfile.TemporaryDirectory(prefix="audio_debug_chunks_") as tmp_dir:
        tmp_path = Path(tmp_dir)
        for chunk_index, start_time in enumerate(range(0, int(duration), chunk_seconds)):
            chunk_duration = min(chunk_seconds, int(math.ceil(duration - start_time)))
            chunk_path = tmp_path / f"chunk_{chunk_index}.mp3"
            LOGGER.info("Extracting chunk %s: start=%ss duration=%ss", chunk_index, start_time, chunk_duration)
            extract_chunk(audio_path, chunk_path, start_time, chunk_duration)

            result = transcribe_with_rotation(chunk_path, keys, model, language)
            chunk_text, chunk_segments, chunk_words = collect_word_level_result(result)
            full_text_parts.append(chunk_text)

            for segment in chunk_segments:
                segment["start"] += start_time
                segment["end"] += start_time
                segment["words"] = adjust_timestamps(segment.get("words", []), start_time)
                raw_segments.append(segment)

            all_words.extend(adjust_timestamps(chunk_words, start_time))

    return " ".join(full_text_parts).strip(), raw_segments, all_words


def clean_token(token: str) -> str:
    return token.lower().strip().strip("\"'.,!?;:()[]{}")


def is_terminal_token(token: str) -> bool:
    return bool(TERMINAL_RE.search(token.strip()))


def is_soft_token(token: str) -> bool:
    return bool(SOFT_RE.search(token.strip()))


def word_pause(words: list[dict], index: int) -> float:
    if index >= len(words) - 1:
        return 0.0
    return max(0.0, words[index + 1]["start"] - words[index]["end"])


def make_segment(words: list[dict], reason: str) -> dict:
    return {
        "start": words[0]["start"],
        "end": words[-1]["end"],
        "text": " ".join(word["word"] for word in words).strip(),
        "word_count": len(words),
        "split_reason": reason,
        "words": words,
    }


def choose_split_index(words: list[dict], ideal_min: int, max_words: int, pause_threshold: float) -> tuple[int, str]:
    upper = min(len(words), max_words)
    candidates: list[tuple[int, int, str]] = []
    tail_after_hard_split = len(words) - upper

    for split_len in range(ideal_min, upper + 1):
        prev_word = words[split_len - 1]["word"]
        next_word = words[split_len]["word"] if split_len < len(words) else ""
        pause = word_pause(words, split_len - 1)

        if is_terminal_token(prev_word):
            candidates.append((100, split_len, "terminal_punctuation"))
        elif is_soft_token(prev_word):
            candidates.append((80, split_len, "soft_punctuation"))
        elif clean_token(next_word) in BREAK_WORDS:
            candidates.append((70, split_len, "before_conjunction"))
        elif clean_token(prev_word) in BREAK_WORDS and split_len > ideal_min:
            candidates.append((60, split_len, "after_conjunction"))
        elif pause >= pause_threshold:
            candidates.append((50, split_len, "pause"))

    if candidates:
        target = min(max_words, max(ideal_min, (ideal_min + max_words) // 2))
        candidates.sort(key=lambda item: (-item[0], abs(item[1] - target)))
        _, split_len, reason = candidates[0]
        return split_len, reason

    if 0 < tail_after_hard_split < ideal_min and len(words) > ideal_min * 2:
        return max(ideal_min, len(words) - ideal_min), "avoid_short_tail"

    return upper, "max_words"


def split_long_sentence(words: list[dict], ideal_min: int, max_words: int, pause_threshold: float) -> list[dict]:
    segments = []
    remaining = words[:]

    while len(remaining) > max_words:
        split_len, reason = choose_split_index(remaining, ideal_min, max_words, pause_threshold)
        segments.append(make_segment(remaining[:split_len], reason))
        remaining = remaining[split_len:]

    if remaining:
        segments.append(make_segment(remaining, "sentence_tail"))

    return segments


def segment_words(
    words: list[dict],
    ideal_min: int,
    max_words: int,
    pause_threshold: float,
    merge_short: bool,
) -> list[dict]:
    sentence_chunks = []
    current = []

    for word in words:
        current.append(word)
        if is_terminal_token(word["word"]):
            sentence_chunks.append(current)
            current = []

    if current:
        sentence_chunks.append(current)

    segments = []
    for chunk in sentence_chunks:
        if len(chunk) <= max_words:
            segments.append(make_segment(chunk, "terminal_punctuation" if is_terminal_token(chunk[-1]["word"]) else "text_end"))
        else:
            segments.extend(split_long_sentence(chunk, ideal_min, max_words, pause_threshold))

    if merge_short:
        segments = merge_short_segments(segments, ideal_min, max_words)

    return segments


def merge_short_segments(segments: list[dict], ideal_min: int, max_words: int) -> list[dict]:
    if not segments:
        return []

    merged = []
    current = segments[0]

    for next_segment in segments[1:]:
        combined_words = current["words"] + next_segment["words"]
        current_is_short = current["word_count"] < ideal_min
        next_is_short = next_segment["word_count"] < ideal_min
        current_has_terminal = is_terminal_token(current["words"][-1]["word"])

        if (current_is_short or next_is_short) and not current_has_terminal and len(combined_words) <= max_words:
            current = make_segment(combined_words, "merged_short_segment")
        else:
            merged.append(current)
            current = next_segment

    merged.append(current)
    return merged


def build_summary(audio_path: Path, full_text: str, raw_segments: list[dict], final_segments: list[dict]) -> dict:
    return {
        "audio": str(audio_path),
        "text_length": len(full_text),
        "raw_segment_count": len(raw_segments),
        "final_segment_count": len(final_segments),
        "avg_words_per_segment": round(
            sum(segment["word_count"] for segment in final_segments) / max(1, len(final_segments)),
            2,
        ),
        "longest_segment_words": max((segment["word_count"] for segment in final_segments), default=0),
    }


def write_outputs(out_dir: Path, audio_path: Path, full_text: str, raw_segments: list[dict], final_segments: list[dict]):
    out_dir.mkdir(parents=True, exist_ok=True)
    summary = build_summary(audio_path, full_text, raw_segments, final_segments)

    (out_dir / "transcript.txt").write_text(full_text + "\n", encoding="utf-8")
    (out_dir / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    (out_dir / "raw_segments.json").write_text(
        json.dumps({"text": full_text, "segments": raw_segments}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (out_dir / "final_segments.json").write_text(
        json.dumps({"text": full_text, "segments": final_segments}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    lines = ["# Audio Transcription Debug", "", "## Summary", ""]
    for key, value in summary.items():
        lines.append(f"- **{key}**: {value}")
    lines.extend(["", "## Final Segments", ""])
    for index, segment in enumerate(final_segments, start=1):
        lines.append(
            f"{index}. [{segment['start']:.2f}s - {segment['end']:.2f}s] "
            f"({segment['word_count']} words, {segment['split_reason']}) {segment['text']}"
        )
    (out_dir / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    LOGGER.info("Wrote debug outputs to %s", out_dir)
    return summary


def parse_args():
    parser = argparse.ArgumentParser(description="Debug local audio transcription and segmentation with Groq word timestamps.")
    parser.add_argument("--audio", default="input_1.mp3", help="Path to local audio file.")
    parser.add_argument("--out-dir", default="debug_outputs/input_1", help="Directory for debug output files.")
    parser.add_argument("--model", default="whisper-large-v3", help="Groq transcription model.")
    parser.add_argument("--language", default="en", help="Transcription language.")
    parser.add_argument("--chunk-seconds", type=int, default=600, help="Chunk duration for files larger than 24MB.")
    parser.add_argument("--ideal-min", type=int, default=8, help="Preferred minimum words per segment.")
    parser.add_argument("--max-words", type=int, default=25, help="Maximum words per segment before splitting.")
    parser.add_argument("--pause-threshold", type=float, default=0.6, help="Pause seconds considered a good split point.")
    parser.add_argument("--no-merge-short", action="store_true", help="Disable short segment merging.")
    parser.add_argument("--verbose", action="store_true", help="Enable debug logging.")
    return parser.parse_args()


def main():
    args = parse_args()
    configure_logging(args.verbose)

    audio_path = Path(args.audio).resolve()
    if not audio_path.exists():
        raise FileNotFoundError(f"Audio file not found: {audio_path}")

    keys = load_api_keys()
    LOGGER.info("Loaded %s Groq key(s) for runtime rotation.", len(keys))

    full_text, raw_segments, words = transcribe_audio(
        audio_path=audio_path,
        keys=keys,
        model=args.model,
        language=args.language,
        chunk_seconds=args.chunk_seconds,
    )
    LOGGER.info("Received %s word-level timestamps from Groq.", len(words))

    final_segments = segment_words(
        words=words,
        ideal_min=args.ideal_min,
        max_words=args.max_words,
        pause_threshold=args.pause_threshold,
        merge_short=not args.no_merge_short,
    )

    summary = write_outputs(
        out_dir=Path(args.out_dir),
        audio_path=audio_path,
        full_text=full_text,
        raw_segments=raw_segments,
        final_segments=final_segments,
    )
    LOGGER.info("Summary: %s", json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
