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

# ======================
# UTILS
# ======================
def split_sentences(text: str):
    """Split text into sentences based on punctuation."""
    sentences = re.split(r'(?<=[.!?])\s+', text)
    return [s.strip() for s in sentences if s.strip()]

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

def split_into_sentences_improved(segment):
    """
    ✅ IMPROVED: Chia câu với proportional timing dựa trên độ dài text
    Thay vì chia đều, ta chia tỉ lệ theo số ký tự
    """
    text = segment['text'].strip()
    sentences = re.split(r'(?<=[.!?])\s+', text)

    results = []
    start = segment['start']
    total_duration = segment['end'] - segment['start']

    if len(sentences) == 1:
        return [segment]

    # ✅ Tính thời gian dựa trên độ dài từng câu
    sentence_lengths = [len(s.strip()) for s in sentences]
    total_chars = sum(sentence_lengths)
    
    if total_chars == 0:
        return [segment]

    current_time = start
    for i, sentence in enumerate(sentences):
        if not sentence.strip():
            continue
        
        # Proportional allocation: thời gian ~ độ dài câu
        sent_duration = (sentence_lengths[i] / total_chars) * total_duration
        
        new_seg = segment.copy()
        new_seg['text'] = sentence.strip()
        new_seg['start'] = current_time
        new_seg['end'] = current_time + sent_duration
        results.append(new_seg)
        
        current_time += sent_duration

    return results

def split_into_sentences(segment):
    """Wrapper để dùng phiên bản improved"""
    return split_into_sentences_improved(segment)

def post_process_segments(segments):
    logger.info(f"Post-processing {len(segments)} segments.")
    merged = merge_segments(segments)

    logger.info(f"Merged {len(merged)} segments.")
    final_segments = []
    for seg in merged:
        split_segs = split_into_sentences(seg)
        final_segments.extend(split_segs)

    logger.info(f"Split {len(final_segments)} segments.")
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

# ✅ IMPROVED: Retry logic với tenacity
@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10),
    reraise=True
)
def transcribe_with_groq(file_path: str):
    """
    ✅ IMPROVED: Transcribe với retry logic và explicit timeout
    """
    logger.info(f"Transcribing {file_path} with Groq Whisper...")
    
    with open(file_path, "rb") as f:
        try:
            transcription = groq_client.audio.transcriptions.create(
                file=f,
                model="whisper-large-v3",
                response_format="verbose_json",
                timeout=300  # 5 minutes timeout
            )
            logger.info(f"Transcription completed successfully. Duration: {transcription.duration}s")
            return transcription
        except Exception as e:
            logger.error(f"Transcription attempt failed: {e}")
            raise

def should_chunk_audio(file_size_bytes):
    """✅ NEW: Kiểm tra có cần split file audio không"""
    max_size = MAX_FILE_SIZE_MB * 1024 * 1024
    return file_size_bytes > max_size

def get_audio_duration(file_path: str) -> float:
    """✅ NEW: Lấy duration của file audio"""
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
    """✅ NEW: Tạo chunk audio từ file gốc"""
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
    """
    ✅ NEW: Transcribe file dài bằng cách chia thành chunks
    Giải pháp cho file > 25MB
    """
    file_size = os.path.getsize(audio_path)
    
    if not should_chunk_audio(file_size):
        logger.info(f"File size {file_size} is under limit, transcribing normally...")
        return transcribe_with_groq(audio_path)
    
    logger.warning(f"File size {file_size} exceeds {MAX_FILE_SIZE_MB}MB, using chunking strategy...")
    
    audio_duration = get_audio_duration(audio_path)
    logger.info(f"Total audio duration: {audio_duration}s")
    
    all_segments = []
    time_offset = 0
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
            
            # Điều chỉnh timestamps với time offset
            for seg in result.segments:
                adjusted_seg = {
                    "start": seg["start"] + chunk_start,  # ✅ Thêm offset thời gian
                    "end": seg["end"] + chunk_start,
                    "text": seg["text"]
                }
                if "words" in seg:
                    adjusted_seg["words"] = seg["words"]
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

def process_audio(ch, lesson_id: str, file_url: str) -> dict:
    local_file_path = f"temp_{lesson_id}"
    audio_path = f"{local_file_path}.mp3"

    try:
        send_progress(ch, lesson_id, 10, "Downloading file...")
        download_file(file_url, local_file_path)

        send_progress(ch, lesson_id, 20, "Extracting audio...")
        extract_audio(local_file_path, audio_path)

        send_progress(ch, lesson_id, 30, "Transcribing...")
        target_path = audio_path if os.path.exists(audio_path) else local_file_path
        
        logger.info(f"Starting Whisper transcription for {lesson_id}")
        
        # ✅ IMPROVED: Dùng chunking strategy nếu file lớn
        transcription_result = transcribe_chunked_audio(target_path)
        
        # Handle kết quả từ chunking (list segments) hoặc normal (object)
        if isinstance(transcription_result, list):
            segments_data = transcription_result
            full_text = " ".join([seg["text"] for seg in segments_data])
        else:
            full_text = transcription_result.text
            segments_data = []
            for seg in transcription_result.segments:
                segments_data.append({
                    "start": seg["start"],
                    "end": seg["end"],
                    "text": seg["text"]
                })

        # ✅ IMPROVED: Post-process để split sentences với proportional timing
        processed_segments = post_process_segments(segments_data)
        
        result_data = {"text": full_text.strip(), "segments": processed_segments}
        return {"status": "COMPLETED", "result": result_data}

    except Exception as e:
        logger.error(f"Error processing audio for {lesson_id}:\n{traceback.format_exc()}")
        return {"status": "FAILED", "error": str(e)}

    finally:
        if os.path.exists(local_file_path):
            os.remove(local_file_path)
        if os.path.exists(audio_path):
            os.remove(audio_path)

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