# Deployment Guide (Linux VPS with Docker)

This guide provides detailed instructions on how to deploy the Video-to-Text API on your Linux VPS using the pre-built Docker image from Docker Hub.

## Prerequisites

1. A Linux VPS (Ubuntu/Debian recommended).
2. **Docker** installed (`sudo apt install docker.io`).
3. **Docker Compose** installed (`sudo apt install docker-compose` or `sudo apt install docker-compose-plugin`).

## Step 1: Transfer File to the VPS
Since the image is hosted on Docker Hub, you no longer need the source code (`main.py`, `Dockerfile`, etc.) on your server. 

You only need to transfer **one file** to your VPS in your desired directory (e.g., `/var/www/video-api`):
* `docker-compose.yml`

## Step 2: Start the Server

1. SSH into your VPS and navigate to the directory where you placed the file:
   ```bash
   cd /var/www/video-api
   ```

2. Run the application in detached mode:
   ```bash
   sudo docker-compose up -d
   ```
   *Note: Docker will automatically pull the `minhtumtn/video-to-text-api:latest` image from Docker Hub and start the server.*

## Step 3: Verify the Deployment

1. Check the logs:
   ```bash
   sudo docker-compose logs -f
   ```
   You should eventually see `Uvicorn running on http://0.0.0.0:8000`.

2. The API UI should now be accessible via your VPS IP address and port 8000:
   * `http://YOUR_VPS_IP:8000`

## Important Notes

* **Model Caching:** The `docker-compose.yml` mounts a local `./models` directory. When `faster-whisper` downloads the AI model weights for the first time, it will save them to this mapped directory. This prevents your VPS from redownloading massive files if the container crashes or restarts.
* **Output Data Persistence:** The `docker-compose.yml` maps an `./outputs` directory. This allows you to access the resulting `Output_*.json` files directly on your VPS disk. Docker will automatically create this folder on your VPS when the first transcription completes.
