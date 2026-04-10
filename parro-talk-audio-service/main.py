import os
import uuid
import json
import asyncio
import subprocess
from fastapi import FastAPI, UploadFile, File, WebSocket, WebSocketDisconnect, BackgroundTasks
from fastapi.responses import HTMLResponse
from faster_whisper import WhisperModel

app = FastAPI(title="Video to Text API")

# Load the faster-whisper model. Using CTranslate2 reduces RAM usage and improves speed.
# "small" is the default size, balancing performance and memory. If the machine is very weak, set to "base".
MODEL_SIZE = "small"
model = WhisperModel(MODEL_SIZE, device="auto", compute_type="int8")

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
                print(f"Error sending message to {task_id}: {e}")

manager = ConnectionManager()
tasks_status = {}

def extract_audio(video_path: str, audio_path: str):
    """
    Extract audio from video using FFmpeg to reduce processing load and RAM usage for Whisper.
    Note: Requires FFmpeg installed and available in system PATH.
    """
    cmd = [
        "ffmpeg", "-i", video_path,
        "-vn", "-acodec", "libmp3lame",
        "-q:a", "2", audio_path, "-y"
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

async def transcribe_task(task_id: str, file_path: str):
    tasks_status[task_id] = "processing"
    
    try:
        # Initial sleep to ensure connection is ready
        await asyncio.sleep(1)
        await manager.send_message(task_id, {"status": "processing", "message": "1. Starting transcription pipeline..."})
        
        audio_path = f"{file_path}.mp3"
        await manager.send_message(task_id, {"status": "processing", "message": "2. Extracting audio from uploaded file..."})
        
        # Perform audio extraction
        extract_audio(file_path, audio_path)
        
        # Fallback to the original file if ffmpeg fails (e.g. ffmpeg not found in PATH)
        target_path = audio_path if os.path.exists(audio_path) else file_path

        await manager.send_message(task_id, {"status": "processing", "message": "3. Transcribing with AI model..."})

        # Process with faster-whisper
        segments, info = model.transcribe(target_path, beam_size=5)
        
        full_text = ""
        segments_data = []
        
        for segment in segments:
            seg_dict = {
                "start": segment.start,
                "end": segment.end,
                "text": segment.text
            }
            segments_data.append(seg_dict)
            full_text += segment.text
            
            # Calculate progress percentage based on total duration
            progress_percent = min((segment.end / info.duration) * 100, 100) if info.duration > 0 else 0
            
            await manager.send_message(task_id, {
                "status": "processing",
                "progress": round(progress_percent, 2),
                "current_segment": seg_dict
            })
            
            # Yield control to the event loop so the WebSocket message can be sent
            await asyncio.sleep(0.01)

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
        
        # Save output result to JSON
        output_dir = "outputs"
        os.makedirs(output_dir, exist_ok=True)
        output_path = os.path.join(output_dir, f"Output_{task_id}.json")
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(result_data, f, ensure_ascii=False, indent=4)
            
    except Exception as e:
        tasks_status[task_id] = "failed"
        await manager.send_message(task_id, {"status": "failed", "error": str(e)})
    finally:
        # Cleanup temporary audio and physical files to free memory and disk space
        if os.path.exists(file_path):
            os.remove(file_path)
        if 'audio_path' in locals() and os.path.exists(audio_path):
            os.remove(audio_path)

@app.post("/upload")
async def upload_video(background_tasks: BackgroundTasks, file: UploadFile = File(...)):
    """API to accept the uploaded file and spawn the background transcription task."""
    task_id = str(uuid.uuid4())
    temp_path = f"temp_{task_id}_{file.filename}"
    
    # Save the file temporarily
    with open(temp_path, "wb") as buffer:
        content = await file.read()
        buffer.write(content)
        
    tasks_status[task_id] = "pending"
    
    # Run the processing task in the background (Non-blocking)
    background_tasks.add_task(transcribe_task, task_id, temp_path)
    
    return {
        "task_id": task_id,
        "message": "File uploaded successfully. Use the task_id to track progress via WebSocket.",
        "websocket_url": f"ws://localhost:8000/ws/progress/{task_id}"
    }

@app.websocket("/ws/progress/{task_id}")
async def websocket_endpoint(websocket: WebSocket, task_id: str):
    """WebSocket endpoint to push real-time events to the client."""
    await manager.connect(task_id, websocket)
    try:
        while True:
            data = await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(task_id)

@app.get("/")
def get_ui():
    """Serves the test UI HTML."""
    with open("index.html", "r", encoding="utf-8") as f:
        return HTMLResponse(f.read())