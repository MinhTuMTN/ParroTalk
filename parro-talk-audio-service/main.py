import os
import json
import logging
import re
import traceback
import subprocess
import requests
import pika
from faster_whisper import WhisperModel

# ======================
# LOGGING CONFIG
# ======================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("AudioService")

# ======================
# MODEL CONFIG
# ======================
MODEL_SIZE = "small.en"
model = WhisperModel(MODEL_SIZE, device="auto", compute_type="int8")

# ======================
# POST-PROCESSING CONFIG
# ======================
MAX_GAP_SECONDS = 0.5
MIN_WORDS_PER_SEGMENT = 8
MAX_WORDS_PER_SEGMENT = 25
TERMINAL_PUNCTUATIONS = {".", "?", "!"}

# ======================
# AMQP CONFIG
# ======================
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")

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

def split_into_sentences(segment):
    text = segment['text'].strip()

    # Split theo dấu chấm, ?, !
    sentences = re.split(r'(?<=[.!?])\s+', text)

    results = []
    start = segment['start']
    total_duration = segment['end'] - segment['start']

    if len(sentences) == 1:
        return [segment]

    # Chia time đều (approximation)
    avg_duration = total_duration / len(sentences)

    for i, sentence in enumerate(sentences):
        new_seg = segment.copy()
        new_seg['text'] = sentence.strip()
        new_seg['start'] = start + i * avg_duration
        new_seg['end'] = start + (i + 1) * avg_duration
        results.append(new_seg)

    return results

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
        segments, info = model.transcribe(
            target_path,
            beam_size=5,
            vad_filter=True,
            vad_parameters=dict(min_silence_duration_ms=300),
            word_timestamps=True
        )

        full_text = ""
        segments_data = []
        duration = info.duration

        for segment in segments:
            raw_text = segment.text
            words = segment.words if hasattr(segment, 'words') else None
            
            # Send progress update based on current segment timestamp
            progress_pct = 30 + ( (segment.end / duration) * 60 ) # Map 0-duration to 30%-90%
            send_progress(ch, lesson_id, round(min(progress_pct, 95), 2), "In progress...")

            if not words:
                sentences = split_sentences(raw_text)
                for sentence in sentences:
                    seg_dict = {"start": segment.start, "end": segment.end, "text": sentence}
                    segments_data.append(seg_dict)
                    full_text += " " + sentence
            else:
                current_sentence = ""
                current_start = None
                current_end = None

                for w in words:
                    if current_start is None: current_start = w.start
                    current_end = w.end
                    current_sentence += w.word

                    if re.search(r'[.!?]$', w.word.strip()):
                        sent_text = current_sentence.strip()
                        seg_dict = {
                            "start": current_start, 
                            "end": current_end, 
                            "text": sent_text,
                            "words": [{"word": x.word, "start": x.start, "end": x.end} for x in words if x.start >= current_start and x.end <= current_end]
                        }
                        segments_data.append(seg_dict)
                        full_text += (" " + sent_text) if full_text else sent_text
                        current_sentence = ""
                        current_start = None

                if current_sentence.strip():
                    sent_text = current_sentence.strip()
                    seg_dict = {
                        "start": current_start, 
                        "end": current_end, 
                        "text": sent_text,
                        "words": [{"word": x.word, "start": x.start, "end": x.end} for x in words if x.start >= current_start and x.end <= current_end]
                    }
                    segments_data.append(seg_dict)
                    full_text += (" " + sent_text) if full_text else sent_text

        # -----------------------------
        # Post-processing: Merge segments
        # -----------------------------
        segments_data = post_process_segments(segments_data)

        result_data = {"text": full_text.strip(), "segments": segments_data}
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
        # In case of message parsing errors or unexpected failures, nack and don't requeue to send to DLQ
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def main():
    try:
        logger.info("Connecting to RabbitMQ...")
        
        logger.info(f"RABBITMQ_HOST: {RABBITMQ_HOST}")
        logger.info(f"RABBITMQ_USER: {RABBITMQ_USER}")
        logger.info(f"RABBITMQ_PASS: {RABBITMQ_PASS}")
        
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