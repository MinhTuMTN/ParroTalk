# Parro Talk Audio Service

RabbitMQ worker service for downloading lesson audio, preprocessing silence, transcribing with Groq Whisper, post-processing timestamped segments, and publishing the result back to RabbitMQ.

## Project Structure

```text
.
├── app/
│   ├── config.py              # Environment variables, queue names, API key rotation
│   ├── groq_result.py         # Groq SDK result normalization helpers
│   ├── logging_config.py      # Shared logger
│   ├── media.py               # File and YouTube audio download helpers
│   ├── messaging.py           # Progress event publishing
│   ├── preprocessing.py       # FFmpeg silence detection, trimming, timestamp remapping
│   ├── segment_processing.py  # Segment merge/split post-processing
│   ├── service.py             # End-to-end lesson audio workflow
│   └── worker.py              # RabbitMQ consumer entrypoint
├── main.py                    # Thin executable entrypoint
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

## Requirements

- Python 3.10+
- FFmpeg and FFprobe available in `PATH`
- RabbitMQ
- Groq API key

Install Python dependencies:

```bash
pip install -r requirements.txt
```

## Environment

Create `.env` from `.env.example` or export these variables:

```bash
GROQ_API_KEYS=your_groq_key
RAPIDAPI_KEYS=your_rapidapi_key
RABBITMQ_HOST=localhost
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
```

`GROQ_API_KEYS` and `RAPIDAPI_KEYS` can be either comma-separated strings or JSON arrays.

## Running

```bash
python main.py
```

The worker consumes messages from `audio-processing-queue` and publishes progress/final results to `audio-result-queue`.

Expected input message:

```json
{
  "lessonId": "lesson-id",
  "fileUrl": "https://example.com/audio-or-video.mp3"
}
```

Completed result shape:

```json
{
  "lessonId": "lesson-id",
  "status": "COMPLETED",
  "result": {
    "text": "Full transcript",
    "segments": [
      {
        "start": 0.0,
        "end": 2.5,
        "text": "Segment text",
        "words": []
      }
    ]
  }
}
```

## Docker

```bash
docker build -t parro-talk-audio-service .
docker run --env-file .env parro-talk-audio-service
```
