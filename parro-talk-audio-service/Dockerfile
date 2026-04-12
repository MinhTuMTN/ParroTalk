FROM python:3.10-slim

# =========================
# System dependencies
# =========================
RUN apt-get update && apt-get install -y \
    ffmpeg \
    git \
    && rm -rf /var/lib/apt/lists/*

# =========================
# Workdir
# =========================
WORKDIR /app

# =========================
# Install Python deps
# =========================
COPY requirements.txt .

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# =========================
# Copy source code
# =========================
COPY . .

# =========================
# Faster Whisper cache models
# =========================
ENV HF_HOME=/app/models
ENV TRANSFORMERS_CACHE=/app/models

# =========================
# Optional: reduce CPU usage issues
# =========================
ENV PYTHONUNBUFFERED=1

# =========================
# If you still expose API (optional)
# =========================
EXPOSE 8000

# =========================
# RUN MODE (choose one)
# =========================

# Case 1: FastAPI server
# CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

# Case 2: RabbitMQ main
CMD ["python", "main.py"]
