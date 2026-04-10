# Video to Text API

A high-performance API to transcribe Video and Audio files to Text, featuring real-time translation progress tracking via WebSocket. Built using FastAPI and `faster-whisper` to optimize RAM usage, making it ideal for running on machines with limited hardware resources.

## Installation

1. Ensure you have Python 3.8+ installed.
2. Install `ffmpeg` on your system and ensure it is available in your system PATH.
3. Install the required Python packages:

```bash
pip install -r requirements.txt
```

## Running the API

Start the API server simply by running Uvicorn:

```bash
uvicorn main:app --reload
```

The server will be available at: `http://localhost:8000`

---

## API Documentation

### 1. File Upload & Processing Endpoint

Upload a physical video or audio file to start a transcription task. The transcription will run in the background.

* **URL**: `/upload`
* **Method**: `POST`
* **Content-Type**: `multipart/form-data`

#### Parameters

| Field | Type | Description |
|-------|------|-------------|
| `file` | File | The video or audio file to be transcribed (e.g., .mp4, .mkv, .mp3, .wav). |

#### Success Response

* **Code**: 200 OK
* **Content**:
```json
{
  "task_id": "b0b91f1b-3fde-4b72-81ce-0857987d17af",
  "message": "File uploaded successfully. Use the task_id to track progress via WebSocket.",
  "websocket_url": "ws://localhost:8000/ws/progress/b0b91f1b-3fde-4b72-81ce-0857987d17af"
}
```

### 2. WebSocket Progress Tracking

Track the real-time progress of a transcription task using WebSocket. The server will stream status and transcribed segments as they become available.

* **URL**: `/ws/progress/{task_id}`
* **Protocol**: `ws://` (or `wss://` if hosted behind HTTPS)

#### Connection Lifecycle
Once connected to the WebSocket endpoint using the `task_id` returned from `/upload`, the server will begin emitting event messages.

#### Sample Event Messages (JSON)

**1. Processing / Progress Event**
```json
{
  "status": "processing",
  "message": "3. Transcribing with AI model...",
  "progress": 45.5,
  "current_segment": {
    "start": 12.5,
    "end": 17.2,
    "text": " Example transcribed text from the video..."
  }
}
```

**2. Completion Event**
```json
{
  "status": "completed",
  "progress": 100,
  "result": {
    "text": "Full transcribed paragraph goes here...",
    "segments": [ ...array of segments... ]
  }
}
```

**3. Error Event**
```json
{
  "status": "failed",
  "error": "Detailed error message here"
}
```

---

## How to Test

1. Open your web browser and navigate to `http://localhost:8000`. This will serve the default testing UI we provided.
2. Select any video or audio file from your computer and click **Upload & Transcribe**.
3. A progress bar will appear, and you will see the transcribed text dynamically appearing on-screen as the AI decodes the audio.
4. Once completed, a `.json` file containing the full result will automatically be generated in the project root directory (e.g. `Output_[task_id].json`).

## Customizing the Model

To adjust performance, you can pick a different model in `main.py`:
```python
MODEL_SIZE = "small" # Defaults to "small". Switch to "base" for extremely weak machines, or "medium" for better accuracy.
```
