# ParroTalk 🦜

ParroTalk is a production-ready English dictation web application designed to help users improve their listening and writing skills through interactive practice. Users can upload audio/video files, get them automatically transcribed using AI, and practice dictation with real-time feedback.

## 🚀 Features

- **Automated AI Transcription**: Integrated with OpenAI Whisper for high-accuracy word-level transcription and timestamps.
- **Interactive Dictation Player**: 
  - Dynamic loop clipping (rewinds automatically to the current sentence).
  - Real-time correctness validation (character-by-character).
  - Playback speed control (0.5x - 1.5x).
  - Contextual hints and masking.
- **Lesson Library**: Manage and browse your processed audio/video lessons with status tracking (In Progress, Completed).
- **Progress Persistence**: Automatically saves your learning progress locally for each lesson.
- **Real-time Processing**: Stream transcription progress directly from the backend via Server-Sent Events (SSE).

## 🛠 Tech Stack

### Frontend
- **Next.js 14+** (App Router)
- **Tailwind CSS** (Styling)
- **Lucide React** (Icons)
- **TypeScript**

### Backend
- **Spring Boot 3.4+**
- **Spring Data JPA** & **PostgreSQL**
- **Cloudinary** (Media storage)
- **WebSockets/SSE** (Real-time updates)

### Audio Service
- **Python / FastAPI**
- **OpenAI Whisper** (Speech-to-Text)

## 📁 Project Structure

```text
/
├── parro-talk-frontend/    # Next.js Application
├── parro-talk-backend/     # Spring Boot API
└── parro-talk-audio-service/# Python AI Service
```

## ⚙️ Getting Started

### Prerequisites
- Node.js 18+
- Java 17+
- Python 3.9+
- PostgreSQL
- Cloudinary Account

### Backend Setup
1. Navigate to `parro-talk-backend`.
2. Copy `src/main/resources/application.yml.example` to `application.yml`.
3. Update `application.yml` with your PostgreSQL and Cloudinary credentials.
4. Run `./mvnw spring-boot:run`.

### Frontend Setup
1. Navigate to `parro-talk-frontend`.
2. Run `npm install`.
3. Run `npm run dev`.
4. Open [http://localhost:3000](http://localhost:3000).

### Audio Service Setup
1. Navigate to `parro-talk-audio-service`.
2. Install dependencies (e.g., `pip install -r requirements.txt`).
3. Launch the service (e.g., using `uvicorn main:app`).

## 📖 API Documentation
Refer to [API_DOCS.md](./API_DOCS.md) for detailed information on the available REST endpoints and real-time streams.

## 📝 License
[MIT](LICENSE)
