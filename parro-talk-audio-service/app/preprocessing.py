import os
import re
import subprocess

from app.logging_config import logger


def get_audio_duration(file_path: str) -> float:
    try:
        result = subprocess.run(
            [
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file_path,
            ],
            capture_output=True,
            text=True,
            timeout=10,
        )
        return float(result.stdout.strip())
    except Exception as exc:
        logger.warning("Could not get audio duration: %s", exc)
        return 0


def extract_audio_chunk(input_path: str, output_path: str, start_time: int, duration: int):
    cmd = [
        "ffmpeg", "-i", input_path,
        "-ss", str(start_time),
        "-t", str(duration),
        "-acodec", "libmp3lame",
        "-q:a", "2",
        output_path, "-y",
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    logger.info("Created chunk: %s (start=%ss, duration=%ss)", output_path, start_time, duration)


def detect_silence(audio_path: str, threshold: str = "-50dB", min_duration: float = 1.0):
    logger.info("Detecting silence in %s (threshold=%s, min_duration=%ss)...", audio_path, threshold, min_duration)
    cmd = [
        "ffmpeg", "-i", audio_path,
        "-af", f"silencedetect=noise={threshold}:d={min_duration}",
        "-f", "null", "-",
    ]
    result = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)

    silences = []
    current_start = None

    for line in result.stderr.splitlines():
        if "silence_start:" in line:
            match = re.search(r"silence_start:\s+([\d\.]+)", line)
            if match:
                current_start = float(match.group(1))
        elif "silence_end:" in line:
            match = re.search(r"silence_end:\s+([\d\.]+)", line)
            if match and current_start is not None:
                end_time = float(match.group(1))
                silences.append({"start": current_start, "end": end_time})
                current_start = None

    logger.info("Detected %s silence intervals.", len(silences))
    return silences


def build_speech_segments(silences: list, total_duration: float, min_speech_duration: float = 0.3):
    speech_segments = []
    current_time = 0.0

    for silence in silences:
        if silence["start"] > current_time:
            speech_duration = silence["start"] - current_time
            if speech_duration >= min_speech_duration:
                speech_segments.append({
                    "original_start": current_time,
                    "original_end": silence["start"],
                })
        current_time = silence["end"]

    if current_time < total_duration:
        speech_duration = total_duration - current_time
        if speech_duration >= min_speech_duration:
            speech_segments.append({
                "original_start": current_time,
                "original_end": total_duration,
            })

    logger.info("Built %s speech segments.", len(speech_segments))
    return speech_segments


def build_time_mapping(speech_segments: list):
    mapping = []
    processed_time = 0.0

    for segment in speech_segments:
        duration = segment["original_end"] - segment["original_start"]
        mapping.append({
            "processed_start": processed_time,
            "processed_end": processed_time + duration,
            "original_start": segment["original_start"],
            "original_end": segment["original_end"],
        })
        processed_time += duration

    return mapping


def create_processed_audio(audio_path: str, speech_segments: list, output_path: str):
    if len(speech_segments) == 0:
        raise ValueError("No speech segments found to process.")

    logger.info("Extracting and merging %s speech segments into %s...", len(speech_segments), output_path)
    filter_script_path = f"{output_path}_filter.txt"
    filter_str = ""

    for i, segment in enumerate(speech_segments):
        start = segment["original_start"]
        end = segment["original_end"]
        logger.debug(
            "Segment %s: original=[%.3f, %.3f]s",
            i,
            segment["original_start"],
            segment["original_end"],
        )
        filter_str += f"[0:a]atrim=start={start}:end={end},asetpts=PTS-STARTPTS[a{i}];\n"

    concat_inputs = "".join([f"[a{i}]" for i in range(len(speech_segments))])
    filter_str += f"{concat_inputs}concat=n={len(speech_segments)}:v=0:a=1[outa]"

    with open(filter_script_path, "w") as file:
        file.write(filter_str)

    cmd = [
        "ffmpeg", "-i", audio_path,
        "-filter_complex_script", filter_script_path,
        "-map", "[outa]",
        "-q:a", "2",
        output_path, "-y",
    ]

    try:
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        logger.info("Successfully created silence-removed audio: %s", output_path)
    finally:
        if os.path.exists(filter_script_path):
            os.remove(filter_script_path)


def remap_timestamp(processed_time: float, mapping: list) -> float:
    if not mapping:
        return processed_time

    for segment in mapping:
        if segment["processed_start"] <= processed_time <= segment["processed_end"]:
            processed_duration = segment["processed_end"] - segment["processed_start"]
            if processed_duration == 0:
                return segment["original_start"]

            ratio = (processed_time - segment["processed_start"]) / processed_duration
            original_duration = segment["original_end"] - segment["original_start"]
            return segment["original_start"] + (ratio * original_duration)

    if processed_time < mapping[0]["processed_start"]:
        return mapping[0]["original_start"]

    if processed_time > mapping[-1]["processed_end"]:
        return mapping[-1]["original_end"]

    return processed_time


def remap_segments(segments_data: list, mapping: list):
    logger.info("Remapping chunk timestamps to original audio timeline.")
    for segment in segments_data:
        orig_start = remap_timestamp(segment["start"], mapping)
        orig_end = remap_timestamp(segment["end"], mapping)
        segment["start"] = orig_start
        segment["end"] = max(orig_start, orig_end)

        for word in segment.get("words", []):
            word_start = remap_timestamp(word["start"], mapping)
            word_end = remap_timestamp(word["end"], mapping)
            word["start"] = word_start
            word["end"] = max(word_start, word_end)
