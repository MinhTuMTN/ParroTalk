import os
import json
import logging
import re
import traceback
import subprocess
import requests
import pika
from groq import Groq
from tenacity import retry, stop_after_attempt, wait_exponential
import itertools
import time

# ======================
# LOGGING CONFIG
# ======================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("AudioService")

# ======================
# POST-PROCESSING CONFIG
# ======================
MAX_GAP_SECONDS = 0.5
MIN_WORDS_PER_SEGMENT = 8
MAX_WORDS_PER_SEGMENT = 25
TERMINAL_PUNCTUATIONS = {".", "?", "!"}

# ✅ NEW: Config cho long-form audio
MAX_FILE_SIZE_MB = 25
AUDIO_CHUNK_DURATION = 600  # 10 minutes per chunk

# ======================
# AMQP CONFIG
# ======================
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")

GROQ_API_KEY = os.getenv("GROQ_API_KEY", "guest")
groq_client = Groq(api_key=GROQ_API_KEY)

PROCESSING_QUEUE = "audio-processing-queue"
RESULT_QUEUE = "audio-result-queue"

def load_rapidapi_keys():
    """Load RapidAPI keys from env (JSON array or comma-separated string)."""
    raw = (os.getenv("RAPIDAPI_KEYS") or os.getenv("RAPIDAPI_KEY") or "").strip()
    if not raw:
        raise ValueError("Missing RAPIDAPI_KEYS (or RAPIDAPI_KEY) environment variable")

    if raw.startswith("["):
        try:
            keys = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ValueError("RAPIDAPI_KEYS must be valid JSON when using array format") from exc
    else:
        keys = [key.strip() for key in raw.split(",") if key.strip()]

    if not isinstance(keys, list) or not all(isinstance(key, str) and key.strip() for key in keys):
        raise ValueError("RAPIDAPI_KEYS must contain one or more non-empty strings")

    return [key.strip() for key in keys]

# Rotate API keys loaded from env
RAPIDAPI_KEYS = load_rapidapi_keys()
RAPIDAPI_HOST = "youtube-mp3-audio-video-downloader.p.rapidapi.com"

# Iterator to rotate keys
_key_cycle = itertools.cycle(RAPIDAPI_KEYS)
logger.info(f"Loaded {len(RAPIDAPI_KEYS)} RapidAPI key(s) for rotation.")

# ======================
# UTILS
# ======================
def should_merge(prev, curr):
    gap = curr['start'] - prev['end']

    prev_text = prev['text'].strip()
    curr_text = curr['text'].strip()

    prev_words = len(prev_text.split())
    curr_words = len(curr_text.split())

    # Stop if previous segment is too long
    if prev_words >= MAX_WORDS_PER_SEGMENT:
        return False

    # Rule 1: Short gap
    if gap < MAX_GAP_SECONDS:
        return True

    # Rule 2: Sentence not end + not too long
    if prev_text and prev_text[-1] not in TERMINAL_PUNCTUATIONS:
        if prev_words + curr_words <= MAX_WORDS_PER_SEGMENT:
            return True

    # Rule 3: If previous or current segment is too short, merge them
    if (prev_words < MIN_WORDS_PER_SEGMENT or curr_words < MIN_WORDS_PER_SEGMENT):
        if prev_words + curr_words <= MAX_WORDS_PER_SEGMENT:
            return True

    return False

def merge_segments(segments):
    """Merge fragmented segments into natural sentence-level chunks."""
    if not segments:
        return []
    
    merged = []
    current = segments[0].copy()
    merge_count = 0
    
    for i in range(1, len(segments)):
        next_seg = segments[i]
        
        if should_merge(current, next_seg):
            # Update text
            current['text'] = current['text'].strip() + " " + next_seg['text'].strip()
            # Update end time
            current['end'] = next_seg['end']
            # Merge words if they exist
            if 'words' in current and 'words' in next_seg:
                current['words'].extend(next_seg['words'])
            elif 'words' in next_seg:
                current['words'] = next_seg['words'].copy()
            
            merge_count += 1
        else:
            merged.append(current)
            current = next_seg.copy()
            
    merged.append(current)
    if merge_count > 0:
        logger.info(f"Post-processing: Merged {merge_count} fragments into {len(merged)} final segments.")
        
    return merged

def post_process_segments(segments):
    logger.info(f"Post-processing {len(segments)} segments.")
    merged = merge_segments(segments)
    logger.info(f"Result: {len(merged)} segments after merge (timestamps preserved from Whisper).")
    return merged

def split_long_segments(segments, max_words=15, ideal_min=5):
    if not segments:
        return []

    final_segments = []
    logger.info(f"Checking {len(segments)} segments for smart splitting...")

    break_conjunctions = {'and', 'but', 'so', 'because', 'although'}

    for seg in segments:
        words_data = seg.get('words', [])
        text = seg.get('text', '').strip()

        if not words_data:
            # fallback giữ nguyên
            final_segments.append(seg)
            continue

        if len(words_data) <= max_words:
            final_segments.append(seg)
            continue

        current_chunk = []

        for i, w in enumerate(words_data):
            current_chunk.append(w)

            word_text = w['word'].strip()
            clean = word_text.lower().strip(".,?!;:-")

            next_word = ""
            if i < len(words_data) - 1:
                next_word = words_data[i+1]['word'].strip()

            # ===== RULES =====
            is_sentence_end = (
                any(p in word_text for p in {'.', '?', '!'})
                and next_word
                and next_word[0].isupper()
            )

            is_conjunction = clean in break_conjunctions

            pause = 0
            if i < len(words_data) - 1:
                pause = words_data[i+1]['start'] - w['end']
            has_pause = pause > 0.6

            too_long = len(current_chunk) >= max_words

            should_split = False

            if len(current_chunk) >= ideal_min:
                if is_sentence_end or has_pause or too_long or is_conjunction:
                    should_split = True

            if i == len(words_data) - 1:
                should_split = True

            if should_split:
                chunk_text = " ".join([cw['word'] for cw in current_chunk])
                final_segments.append({
                    "start": current_chunk[0]['start'],
                    "end": current_chunk[-1]['end'],
                    "text": chunk_text,
                    "words": current_chunk.copy()
                })
                current_chunk = []

    logger.info(f"Split completed: {len(final_segments)} segments.")
    return final_segments

def extract_audio(video_path: str, audio_path: str):
    """Extract audio using FFmpeg (blocking function)."""
    logger.info(f"Extracting audio from {video_path} to {audio_path}")
    cmd = [
        "ffmpeg", "-i", video_path,
        "-vn", "-acodec", "libmp3lame",
        "-q:a", "2", audio_path, "-y"
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def download_file(url: str, local_path: str):
    logger.info(f"Downloading file from {url}")
    with requests.get(url, stream=True) as r:
        r.raise_for_status()
        with open(local_path, 'wb') as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)
    logger.info("Download completed.")

def is_youtube_url(url: str) -> bool:
    youtube_regex = r'(https?://)?(www\.)?(youtube|youtu|youtube-nocookie)\.(com|be)/(watch\?v=|embed/|v/|.+\?v=)?([^&=%\?]{11})'
    return bool(re.match(youtube_regex, url))

def _get_next_key():
    return next(_key_cycle)

def download_youtube_audio(url: str, output_path: str):
    """Download audio from YouTube using RapidAPI with key rotation."""
    logger.info(f"Downloading YouTube audio from {url}")

    # Extract video ID từ URL
    if "v=" in url:
        video_id = url.split("v=")[-1].split("&")[0]
    elif "youtu.be/" in url:
        video_id = url.split("youtu.be/")[-1].split("?")[0]
    else:
        video_id = url.strip("/").split("/")[-1]

    last_error = None

    for _ in range(len(RAPIDAPI_KEYS)):
        api_key = _get_next_key()
        logger.info(f"Using API key: ...{api_key[-6:]}")

        try:
            # Bước 1: Lấy download link
            response = requests.get(
                f"https://{RAPIDAPI_HOST}/get_mp3_download_link/{video_id}",
                headers={
                    "x-rapidapi-key": api_key,
                    "x-rapidapi-host": RAPIDAPI_HOST,
                    "Content-Type": "application/json"
                },
                params={
                    "quality": "high",
                    "wait_until_the_file_is_ready": "false"
                },
                timeout=60
            )

            if response.status_code == 429:
                logger.warning(f"Rate limit hit for key ...{api_key[-6:]}, trying next key")
                last_error = "Rate limit"
                continue

            data = response.json()
            logger.info(f"API response: {data}")

            download_url = data.get("file") or data.get("reserved_file") or data.get("download_url") or data.get("url")
            if not download_url:
                raise Exception(f"Không tìm thấy download URL trong response: {data}")

            # Bước 2: Poll cho đến khi file sẵn sàng (tối đa 5 phút)
            logger.info(f"Waiting for file to be ready: {download_url}")
            max_wait = 300
            interval = 10
            elapsed = 0

            while elapsed < max_wait:
                check = requests.head(download_url, timeout=10)
                if check.status_code == 200:
                    logger.info(f"File ready after {elapsed}s")
                    break
                logger.info(f"File not ready yet (HTTP {check.status_code}), waiting {interval}s... ({elapsed}/{max_wait}s)")
                time.sleep(interval)
                elapsed += interval
            else:
                raise Exception(f"File không sẵn sàng sau {max_wait}s")

            # Bước 3: Download file
            logger.info(f"Downloading from: {download_url}")
            audio_response = requests.get(download_url, stream=True, timeout=120)
            audio_response.raise_for_status()

            final_path = output_path if output_path.endswith(".mp3") else output_path + ".mp3"
            with open(final_path, "wb") as f:
                for chunk in audio_response.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)

            if final_path != output_path and not os.path.exists(output_path):
                os.rename(final_path, output_path)

            logger.info("YouTube audio download completed.")
            return

        except Exception as e:
            last_error = str(e)
            logger.error(f"Error with key ...{api_key[-6:]}: {e}")
            continue

    raise Exception(f"Tất cả API keys đều thất bại. Lỗi cuối: {last_error}")

def send_progress(ch, lesson_id, progress, step):
    payload = {
        "lessonId": lesson_id,
        "status": "PROGRESS",
        "progress": progress,
        "message": step
    }
    logger.info(f"Sending progress for lessonId: {lesson_id}, progress: {progress}, step: {step}")
    ch.basic_publish(
        exchange='',
        routing_key=RESULT_QUEUE,
        body=json.dumps(payload)
    )

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10),
    reraise=True
)
def transcribe_with_groq(file_path: str):
    """Transcribe với retry logic và explicit timeout"""
    logger.info(f"Transcribing {file_path} with Groq Whisper...")
    
    with open(file_path, "rb") as f:
        try:
            transcription = groq_client.audio.transcriptions.create(
                file=f,
                model="whisper-large-v3",
                response_format="verbose_json",
                timestamp_granularities=["segment", "word"],
                timeout=300
            )
            logger.info(f"Transcription completed successfully. Duration: {transcription.duration}s")
            return transcription
        except Exception as e:
            logger.error(f"Transcription attempt failed: {e}")
            raise

def should_chunk_audio(file_size_bytes):
    """Kiểm tra có cần split file audio không"""
    max_size = MAX_FILE_SIZE_MB * 1024 * 1024
    return file_size_bytes > max_size

def get_audio_duration(file_path: str) -> float:
    """Lấy duration của file audio"""
    try:
        result = subprocess.run(
            ['ffprobe', '-v', 'error', '-show_entries', 
             'format=duration', '-of', 'default=noprint_wrappers=1:nokey=1:nokey=1', 
             file_path],
            capture_output=True,
            text=True,
            timeout=10
        )
        return float(result.stdout.strip())
    except Exception as e:
        logger.warning(f"Could not get audio duration: {e}")
        return 0

def extract_audio_chunk(input_path: str, output_path: str, start_time: int, duration: int):
    """Tạo chunk audio từ file gốc"""
    cmd = [
        "ffmpeg", "-i", input_path,
        "-ss", str(start_time),
        "-t", str(duration),
        "-acodec", "libmp3lame",
        "-q:a", "2",
        output_path, "-y"
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    logger.info(f"Created chunk: {output_path} (start={start_time}s, duration={duration}s)")

def transcribe_chunked_audio(audio_path: str):
    """Transcribe file dài bằng cách chia thành chunks"""
    file_size = os.path.getsize(audio_path)
    
    if not should_chunk_audio(file_size):
        logger.info(f"File size {file_size} is under limit, transcribing normally...")
        return transcribe_with_groq(audio_path)
    
    logger.warning(f"File size {file_size} exceeds {MAX_FILE_SIZE_MB}MB, using chunking strategy...")
    
    audio_duration = get_audio_duration(audio_path)
    logger.info(f"Total audio duration: {audio_duration}s")
    
    all_segments = []
    chunk_index = 0
    
    # Tính số chunks cần thiết
    num_chunks = int(audio_duration / AUDIO_CHUNK_DURATION) + (1 if audio_duration % AUDIO_CHUNK_DURATION else 0)
    logger.info(f"Will split into {num_chunks} chunks of {AUDIO_CHUNK_DURATION}s each")
    
    for chunk_start in range(0, int(audio_duration), AUDIO_CHUNK_DURATION):
        chunk_end = min(chunk_start + AUDIO_CHUNK_DURATION, audio_duration)
        chunk_duration = chunk_end - chunk_start
        
        chunk_path = f"{audio_path}.chunk_{chunk_index}.mp3"
        
        try:
            # Tạo chunk
            logger.info(f"Extracting chunk {chunk_index}: {chunk_start}s - {chunk_end}s")
            extract_audio_chunk(audio_path, chunk_path, chunk_start, int(chunk_duration))
            
            # Transcribe chunk
            logger.info(f"Transcribing chunk {chunk_index}...")
            result = transcribe_with_groq(chunk_path)

            # Handle cả dict lẫn object tuỳ version Groq SDK
            for seg in result.segments:
                if isinstance(seg, dict):
                    adjusted_seg = {
                        "start": seg["start"] + chunk_start,
                        "end": seg["end"] + chunk_start,
                        "text": seg["text"]
                    }
                    if "words" in seg and seg["words"]:
                        adjusted_words = []
                        for w in seg["words"]:
                            w_dict = w if isinstance(w, dict) else {"word": w.word, "start": w.start, "end": w.end}
                            adjusted_words.append({
                                "word": w_dict["word"],
                                "start": w_dict["start"] + chunk_start,
                                "end": w_dict["end"] + chunk_start
                            })
                        adjusted_seg["words"] = adjusted_words
                else:
                    adjusted_seg = {
                        "start": seg.start + chunk_start,
                        "end": seg.end + chunk_start,
                        "text": seg.text
                    }
                    if hasattr(seg, "words") and seg.words:
                        adjusted_words = []
                        for w in seg.words:
                            w_dict = w if isinstance(w, dict) else {"word": w.word, "start": w.start, "end": w.end}
                            adjusted_words.append({
                                "word": w_dict["word"],
                                "start": w_dict["start"] + chunk_start,
                                "end": w_dict["end"] + chunk_start
                            })
                        adjusted_seg["words"] = adjusted_words
                all_segments.append(adjusted_seg)
            
            logger.info(f"Chunk {chunk_index} completed: {len(result.segments)} segments")
            
        except Exception as e:
            logger.error(f"Error processing chunk {chunk_index}: {e}")
            raise
        finally:
            # Clean up chunk file
            if os.path.exists(chunk_path):
                os.remove(chunk_path)
        
        chunk_index += 1
    
    logger.info(f"Chunked transcription completed: {len(all_segments)} total segments")
    return all_segments

# ======================
# AUDIO PREPROCESSING
# ======================

def detect_silence(audio_path: str, threshold: str = "-50dB", min_duration: float = 1.0):
    """
    ✅ FIX 1: Detect silence intervals using FFmpeg silencedetect.
    Returns a list of dicts: [{'start': float, 'end': float}]
    Params:
      - threshold: -50dB (stricter than -40dB to avoid detecting speech as silence)
      - min_duration: 1.0s (ignore brief pauses between words, only detect real gaps)
    """
    logger.info(f"Detecting silence in {audio_path} (threshold={threshold}, min_duration={min_duration}s)...")
    cmd = [
        "ffmpeg", "-i", audio_path,
        "-af", f"silencedetect=noise={threshold}:d={min_duration}",
        "-f", "null", "-"
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
                silences.append({'start': current_start, 'end': end_time})
                current_start = None
                
    logger.info(f"Detected {len(silences)} silence intervals.")
    return silences

def build_speech_segments(silences: list, total_duration: float, min_speech_duration: float = 0.3):
    """Convert silence intervals to speech segments."""
    speech_segments = []
    current_time = 0.0
    
    for sil in silences:
        if sil['start'] > current_time:
            speech_dur = sil['start'] - current_time
            if speech_dur >= min_speech_duration:
                speech_segments.append({
                    'original_start': current_time,
                    'original_end': sil['start']
                })
        current_time = sil['end']
        
    if current_time < total_duration:
        speech_dur = total_duration - current_time
        if speech_dur >= min_speech_duration:
            speech_segments.append({
                'original_start': current_time,
                'original_end': total_duration
            })
            
    logger.info(f"Built {len(speech_segments)} speech segments.")
    return speech_segments

def build_time_mapping(speech_segments: list):
    """Build a mapping from the processed (concatenated) timeline to the original timeline."""
    mapping = []
    processed_time = 0.0
    
    for seg in speech_segments:
        duration = seg['original_end'] - seg['original_start']
        mapping.append({
            'processed_start': processed_time,
            'processed_end': processed_time + duration,
            'original_start': seg['original_start'],
            'original_end': seg['original_end']
        })
        processed_time += duration
        
    return mapping

def create_processed_audio(audio_path: str, speech_segments: list, output_path: str):
    """
    ✅ FIX 2: Trim out the silences and concatenate the speech segments.
    Adds 100ms padding on both sides to avoid cutting off speech edges.
    """
    if len(speech_segments) == 0:
        raise ValueError("No speech segments found to process.")
    
    PADDING = 0 # ✅ 100ms padding on both sides to avoid cutting off speech edges
    
    logger.info(f"Extracting and merging {len(speech_segments)} speech segments into {output_path}...")
    filter_script_path = f"{output_path}_filter.txt"
    filter_str = ""
    
    # Generate atrim filters for each segment with padding
    for i, seg in enumerate(speech_segments):
        # start = max(0, seg['original_start'] - PADDING)  # ✅ min 0
        start = seg['original_start']
        # end = seg['original_end'] + PADDING
        end = seg['original_end']
        logger.debug(f"Segment {i}: original=[{seg['original_start']:.3f}, {seg['original_end']:.3f}]s, padded=[{start:.3f}, {end:.3f}]s")
        filter_str += f"[0:a]atrim=start={start}:end={end},asetpts=PTS-STARTPTS[a{i}];\n"
        
    # Concat all parts
    concat_inputs = "".join([f"[a{i}]" for i in range(len(speech_segments))])
    filter_str += f"{concat_inputs}concat=n={len(speech_segments)}:v=0:a=1[outa]"
    
    with open(filter_script_path, "w") as f:
        f.write(filter_str)
        
    cmd = [
        "ffmpeg", "-i", audio_path,
        "-filter_complex_script", filter_script_path,
        "-map", "[outa]",
        "-q:a", "2",
        output_path, "-y"
    ]
    
    try:
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        logger.info(f"Successfully created silence-removed audio: {output_path}")
    finally:
        if os.path.exists(filter_script_path):
            os.remove(filter_script_path)

def remap_timestamp(processed_time: float, mapping: list) -> float:
    """
    ✅ FIX 3: Remaps a timestamp from the processed timeline back to the original timeline.
    Uses proportional scaling instead of simple offset to handle overlapping segments correctly.
    """
    if not mapping:
        return processed_time
    
    # Binary search for the containing segment
    for seg in mapping:
        if seg['processed_start'] <= processed_time <= seg['processed_end']:
            # ✅ Proportional mapping: ratio = position in processed → same ratio in original
            processed_duration = seg['processed_end'] - seg['processed_start']
            if processed_duration == 0:
                return seg['original_start']
                
            ratio = (processed_time - seg['processed_start']) / processed_duration
            original_duration = seg['original_end'] - seg['original_start']
            return seg['original_start'] + (ratio * original_duration)
    
    # Out of bounds: before first segment
    if processed_time < mapping[0]['processed_start']:
        return mapping[0]['original_start']
    
    # Out of bounds: after last segment
    if processed_time > mapping[-1]['processed_end']:
        return mapping[-1]['original_end']
    
    # Shouldn't reach here, but fallback
    return processed_time

def remap_segments(segments_data: list, mapping: list):
    """Mutate the segments array by remapping start/end timestamps."""
    logger.info("Remapping chunk timestamps to original audio timeline.")
    for seg in segments_data:
        orig_start = remap_timestamp(seg['start'], mapping)
        orig_end = remap_timestamp(seg['end'], mapping)
        seg['start'] = orig_start
        seg['end'] = max(orig_start, orig_end)

def process_audio(ch, lesson_id: str, file_url: str) -> dict:
    local_file_path = f"temp_{lesson_id}"
    audio_path = f"{local_file_path}.mp3"
    processed_target_path = None

    try:
        if is_youtube_url(file_url):
            send_progress(ch, lesson_id, 10, "Extracting YouTube audio...")
            download_youtube_audio(file_url, audio_path)
        else:
            send_progress(ch, lesson_id, 10, "Downloading file...")
            download_file(file_url, local_file_path)
            send_progress(ch, lesson_id, 20, "Extracting audio...")
            extract_audio(local_file_path, audio_path)

        target_path = audio_path if os.path.exists(audio_path) else local_file_path
        
        # ==========================================
        # AUDIO PREPROCESSING (SILENCE REMOVAL)
        # ==========================================
        send_progress(ch, lesson_id, 25, "Preprocessing audio (Silence removal)...")
        audio_duration = get_audio_duration(target_path)
        logger.info(f"Audio duration: {audio_duration:.2f}s")
        
        silences = detect_silence(target_path)
        logger.info(f"🔍 SILENCE DETECTION RESULT: {len(silences)} silence intervals found")
        for i, sil in enumerate(silences):
            logger.info(f"  Silence {i}: [{sil['start']:.3f}s, {sil['end']:.3f}s] ({sil['end']-sil['start']:.3f}s)")
        
        mapping = None
        if silences:
            speech_segments = build_speech_segments(silences, audio_duration)
            logger.info(f"🔍 SPEECH SEGMENTS: {len(speech_segments)} segments")
            for i, seg in enumerate(speech_segments):
                logger.info(f"  Speech {i}: [{seg['original_start']:.3f}s, {seg['original_end']:.3f}s] ({seg['original_end']-seg['original_start']:.3f}s)")
            
            if not speech_segments:
                raise Exception("Audio contains only silence, no speech detected.")
                
            processed_target_path = f"processed_{lesson_id}.mp3"
            create_processed_audio(target_path, speech_segments, processed_target_path)
            mapping = build_time_mapping(speech_segments)
            logger.info(f"🔍 TIME MAPPING created with {len(mapping)} segments")
            for i, m in enumerate(mapping):
                logger.info(f"  Map {i}: processed=[{m['processed_start']:.3f}, {m['processed_end']:.3f}]s → original=[{m['original_start']:.3f}, {m['original_end']:.3f}]s")
            
            final_audio_path = processed_target_path
        else:
            logger.info("No silence detected. Skipping preprocessing step.")
            final_audio_path = target_path

        # ==========================================

        send_progress(ch, lesson_id, 30, "Transcribing...")
        logger.info(f"Starting Whisper transcription for {lesson_id}")
        
        transcription_result = transcribe_chunked_audio(final_audio_path)
        
        # Handle kết quả từ chunking hoặc normal
        if isinstance(transcription_result, list):
            segments_data = transcription_result
            full_text = " ".join([seg["text"] for seg in segments_data])
        else:
            full_text = transcription_result.text
            segments_data = []
            for seg in transcription_result.segments:
                if isinstance(seg, dict):
                    seg_dict = {
                        "start": seg["start"],
                        "end": seg["end"],
                        "text": seg["text"]
                    }
                    if "words" in seg and seg["words"]:
                        adjusted_words = []
                        for w in seg["words"]:
                            w_dict = w if isinstance(w, dict) else {"word": w.word, "start": w.start, "end": w.end}
                            adjusted_words.append({
                                "word": w_dict["word"],
                                "start": w_dict["start"],
                                "end": w_dict["end"]
                            })
                        seg_dict["words"] = adjusted_words
                    segments_data.append(seg_dict)
                else:
                    seg_dict = {
                        "start": seg.start,
                        "end": seg.end,
                        "text": seg.text
                    }
                    if hasattr(seg, "words") and seg.words:
                        adjusted_words = []
                        for w in seg.words:
                            w_dict = w if isinstance(w, dict) else {"word": w.word, "start": w.start, "end": w.end}
                            adjusted_words.append({
                                "word": w_dict["word"],
                                "start": w_dict["start"],
                                "end": w_dict["end"]
                            })
                        seg_dict["words"] = adjusted_words
                    segments_data.append(seg_dict)

        # ==========================================
        # TIMESTAMP REMAPPING
        # ==========================================
        if mapping:
            remap_segments(segments_data, mapping)
        # ==========================================

        # Lưu raw segments (TRƯỚC post-process) để debug
        raw_data = {"text": full_text.strip(), "segments": segments_data}
        with open(f"debug_{lesson_id}_raw.json", "w", encoding="utf-8") as f:
            json.dump(raw_data, f, ensure_ascii=False, indent=2)
        logger.info(f"Saved RAW JSON to debug_{lesson_id}_raw.json")

        # Post-process: chỉ merge
        processed_segments = post_process_segments(segments_data)
        
        # Split again based on exact word boundaries if merged segments are too long
        final_processed_segments = split_long_segments(processed_segments)

        # Lưu processed segments (SAU post-process) để debug
        result_data = {"text": full_text.strip(), "segments": final_processed_segments}
        with open(f"debug_{lesson_id}_processed.json", "w", encoding="utf-8") as f:
            json.dump(result_data, f, ensure_ascii=False, indent=2)
        logger.info(f"Saved PROCESSED JSON to debug_{lesson_id}_processed.json")

        return {"status": "COMPLETED", "result": result_data}

    except Exception as e:
        logger.error(f"Error processing audio for {lesson_id}:\n{traceback.format_exc()}")
        return {"status": "FAILED", "error": str(e)}

    finally:
        if os.path.exists(local_file_path):
            os.remove(local_file_path)
        if os.path.exists(audio_path):
            os.remove(audio_path)
        if processed_target_path and os.path.exists(processed_target_path):
            os.remove(processed_target_path)

def callback(ch, method, properties, body):
    try:
        msg = json.loads(body.decode('utf-8'))
        lesson_id = msg.get("lessonId")
        file_url = msg.get("fileUrl")

        if not lesson_id or not file_url:
            logger.error(f"Invalid message format: {msg}")
            ch.basic_ack(delivery_tag=method.delivery_tag)
            return

        logger.info(f"Received task for lessonId: {lesson_id}")
        
        result_payload = process_audio(ch, lesson_id, file_url)
        result_payload["lessonId"] = lesson_id

        # Publish result back
        ch.basic_publish(
            exchange='',
            routing_key=RESULT_QUEUE,
            body=json.dumps(result_payload),
            properties=pika.BasicProperties(
                delivery_mode=pika.DeliveryMode.Persistent
            )
        )
        logger.info(f"Published result for lessonId: {lesson_id}")
        
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as e:
        logger.error(f"Failed to process message: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def main():
    try:
        logger.info("Connecting to RabbitMQ...")
        
        logger.info(f"RABBITMQ_HOST: {RABBITMQ_HOST}")
        logger.info(f"RABBITMQ_USER: {RABBITMQ_USER}")
        
        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=RABBITMQ_HOST, port=5672, credentials=credentials)
        )
        
        if connection.is_open:
            logger.info("Connected to RabbitMQ successfully!")
        else:
            logger.error("Connection failed!")

        channel = connection.channel()
        logger.info("Channel created")

    except Exception as e:
        logger.exception("Failed to connect to RabbitMQ")
        return

    # Declare queues
    channel.queue_declare(queue=PROCESSING_QUEUE, durable=True, arguments={
        "x-dead-letter-exchange": "",
        "x-dead-letter-routing-key": "audio-processing-dlq"
    })
    logger.info(f"Queue declared: {PROCESSING_QUEUE}")

    channel.queue_declare(queue=RESULT_QUEUE, durable=True)
    logger.info(f"Queue declared: {RESULT_QUEUE}")

    channel.basic_qos(prefetch_count=1)
    logger.info("QoS set (prefetch_count=1)")

    channel.basic_consume(queue=PROCESSING_QUEUE, on_message_callback=callback)
    logger.info(f"Started consuming from queue: {PROCESSING_QUEUE}")

    logger.info(' [*] Waiting for messages. To exit press CTRL+C')

    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        logger.info("Shutting down worker...")
        channel.stop_consuming()
    finally:
        connection.close()
        logger.info("Connection closed")

if __name__ == "__main__":
    main()