import os
import uuid
import json
import asyncio
import subprocess
import re
from fastapi import FastAPI, UploadFile, File, WebSocket, WebSocketDisconnect, BackgroundTasks
from fastapi.responses import HTMLResponse
from faster_whisper import WhisperModel

app = FastAPI(title="Video to Text API")

# ======================
# MODEL CONFIG
# ======================
MODEL_SIZE = "small.en"
model = WhisperModel(MODEL_SIZE, device="auto", compute_type="int8")

# ======================
# UTILS
# ======================
def split_sentences(text: str):
    """Split text into sentences based on punctuation."""
    sentences = re.split(r'(?<=[.!?])\s+', text)
    return [s.strip() for s in sentences if s.strip()]

def extract_audio(video_path: str, audio_path: str):
    """Extract audio using FFmpeg (blocking function)."""
    cmd = [
        "ffmpeg", "-i", video_path,
        "-vn", "-acodec", "libmp3lame",
        "-q:a", "2", audio_path, "-y"
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

# ======================
# WEBSOCKET MANAGER
# ======================
class ConnectionManager:
    def __init__(self):
        self.active_connections = {}

    async def connect(self, task_id: str, websocket: WebSocket):
        await websocket.accept()
        self.active_connections[task_id] = websocket

    def disconnect(self, task_id: str):
        if task_id in self.active_connections:
            del self.active_connections[task_id]

    async def send_message(self, task_id: str, message: dict):
        if task_id in self.active_connections:
            try:
                await self.active_connections[task_id].send_json(message)
            except Exception as e:
                print(f"WebSocket error ({task_id}): {e}")

manager = ConnectionManager()
tasks_status = {}

# ======================
# MAIN PROCESSING TASK
# ======================
async def transcribe_task(task_id: str, file_path: str):
    tasks_status[task_id] = "processing"

    loop = asyncio.get_event_loop()

    try:
        await asyncio.sleep(0.5)

        await manager.send_message(task_id, {
            "status": "starting",
            "message": "1. Starting pipeline..."
        })

        audio_path = f"{file_path}.mp3"

        # ======================
        # EXTRACT AUDIO (NON-BLOCKING)
        # ======================
        await manager.send_message(task_id, {
            "status": "extracting",
            "message": "2. Extracting audio..."
        })

        await loop.run_in_executor(None, extract_audio, file_path, audio_path)

        target_path = audio_path if os.path.exists(audio_path) else file_path

        # ======================
        # TRANSCRIBE (NON-BLOCKING)
        # ======================
        await manager.send_message(task_id, {
            "status": "transcribing",
            "message": "3. Transcribing..."
        })

        segments, info = await loop.run_in_executor(
            None,
            lambda: model.transcribe(
                target_path,
                beam_size=5,
                vad_filter=True,
                vad_parameters=dict(min_silence_duration_ms=300),
                word_timestamps=True
            )
        )

        full_text = ""
        segments_data = []

        # ======================
        # PROCESS SEGMENTS
        # ======================
        for segment in segments:
            if not getattr(segment, "words", None):
                sentences = split_sentences(segment.text)
                for sentence in sentences:
                    seg_dict = {
                        "start": segment.start,
                        "end": segment.end,
                        "text": sentence
                    }

                    segments_data.append(seg_dict)
                    full_text += " " + sentence

                    progress = (
                        min((segment.end / info.duration) * 100, 100)
                        if info.duration > 0 else 0
                    )

                    await manager.send_message(task_id, {
                        "status": "processing",
                        "progress": round(progress, 2),
                        "current_segment": seg_dict
                    })
                continue

            current_sentence = ""
            current_start = None
            current_end = None

            for word_obj in segment.words:
                if current_start is None:
                    current_start = word_obj.start
                current_end = word_obj.end

                current_sentence += word_obj.word

                # Check for end of sentence
                if re.search(r'[.!?]$', word_obj.word.strip()):
                    sent_text = current_sentence.strip()
                    if sent_text:
                        seg_dict = {
                            "start": current_start,
                            "end": current_end,
                            "text": sent_text
                        }
                        segments_data.append(seg_dict)
                        full_text += (" " + sent_text) if full_text else sent_text

                        progress = (
                            min((current_end / info.duration) * 100, 100)
                            if info.duration > 0 else 0
                        )

                        await manager.send_message(task_id, {
                            "status": "processing",
                            "progress": round(progress, 2),
                            "current_segment": seg_dict
                        })

                    current_sentence = ""
                    current_start = None

            # Handle remaining words that don't end with punctuation
            sent_text = current_sentence.strip()
            if sent_text:
                seg_dict = {
                    "start": current_start,
                    "end": current_end,
                    "text": sent_text
                }
                segments_data.append(seg_dict)
                full_text += (" " + sent_text) if full_text else sent_text

                progress = (
                    min((current_end / info.duration) * 100, 100)
                    if info.duration > 0 else 0
                )

                await manager.send_message(task_id, {
                    "status": "processing",
                    "progress": round(progress, 2),
                    "current_segment": seg_dict
                })

        result_data = {
            "text": full_text.strip(),
            "segments": segments_data
        }

        tasks_status[task_id] = "completed"

        await manager.send_message(task_id, {
            "status": "completed",
            "progress": 100,
            "result": result_data
        })

        # ======================
        # SAVE OUTPUT
        # ======================
        os.makedirs("outputs", exist_ok=True)
        output_path = f"outputs/output_{task_id}.json"

        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(result_data, f, ensure_ascii=False, indent=4)

    except Exception as e:
        tasks_status[task_id] = "failed"
        await manager.send_message(task_id, {
            "status": "failed",
            "error": str(e)
        })

    finally:
        # ======================
        # CLEANUP
        # ======================
        if os.path.exists(file_path):
            os.remove(file_path)

        if 'audio_path' in locals() and os.path.exists(audio_path):
            os.remove(audio_path)

# ======================
# API ENDPOINT
# ======================
@app.post("/upload")
async def upload_video(background_tasks: BackgroundTasks, file: UploadFile = File(...)):
    task_id = str(uuid.uuid4())
    temp_path = f"temp_{task_id}_{file.filename}"

    with open(temp_path, "wb") as buffer:
        buffer.write(await file.read())

    tasks_status[task_id] = "pending"

    background_tasks.add_task(transcribe_task, task_id, temp_path)

    return {
        "task_id": task_id,
        "websocket_url": f"ws://100.114.196.98:8000/ws/progress/{task_id}"
    }

# ======================
# WEBSOCKET
# ======================
@app.websocket("/ws/progress/{task_id}")
async def websocket_endpoint(websocket: WebSocket, task_id: str):
    await manager.connect(task_id, websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(task_id)

# ======================
# TEST UI
# ======================
@app.get("/")
def get_ui():
    with open("index.html", "r", encoding="utf-8") as f:
        return HTMLResponse(f.read())