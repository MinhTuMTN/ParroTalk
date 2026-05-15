import re
import subprocess
import time

import requests

from app.config import RAPIDAPI_HOST, RAPIDAPI_KEYS
from app.logging_config import logger


def extract_audio(video_path: str, audio_path: str):
    logger.info("Extracting audio from %s to %s", video_path, audio_path)
    cmd = [
        "ffmpeg", "-i", video_path,
        "-vn", "-acodec", "libmp3lame",
        "-q:a", "2", audio_path, "-y",
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def download_file(url: str, local_path: str):
    logger.info("Downloading file from %s", url)
    with requests.get(url, stream=True) as response:
        response.raise_for_status()
        with open(local_path, "wb") as file:
            for chunk in response.iter_content(chunk_size=8192):
                file.write(chunk)
    logger.info("Download completed.")


def is_youtube_url(url: str) -> bool:
    youtube_regex = (
        r"(https?://)?(www\.)?(youtube|youtu|youtube-nocookie)\.(com|be)/"
        r"(watch\?v=|embed/|v/|.+\?v=)?([^&=%\?]{11})"
    )
    return bool(re.match(youtube_regex, url))


def _extract_video_id(url: str) -> str:
    if "v=" in url:
        return url.split("v=")[-1].split("&")[0]
    if "youtu.be/" in url:
        return url.split("youtu.be/")[-1].split("?")[0]
    return url.strip("/").split("/")[-1]


def download_youtube_audio_v2(url: str, output_path: str):
    logger.info("Downloading YouTube audio from %s", url)
    video_id = _extract_video_id(url)
    last_error = None

    session = requests.Session()
    session.headers.update({
        "x-rapidapi-host": RAPIDAPI_HOST,
        "Content-Type": "application/json",
    })

    for attempt, api_key in enumerate(RAPIDAPI_KEYS):
        try:
            logger.info("Attempt %s/%s with key ...%s", attempt + 1, len(RAPIDAPI_KEYS), api_key[-6:])
            try:
                response = session.get(
                    f"https://{RAPIDAPI_HOST}/get_mp3_download_link/{video_id}",
                    headers={"x-rapidapi-key": api_key},
                    params={"quality": "high", "wait_until_the_file_is_ready": "false"},
                    timeout=90,
                )
            except requests.exceptions.Timeout:
                logger.warning("Key ...%s timeout (90s), trying next key", api_key[-6:])
                last_error = "API timeout"
                time.sleep(5)
                continue

            if response.status_code == 429:
                logger.warning("Rate limit (429) for key ...%s", api_key[-6:])
                last_error = "Rate limit"
                time.sleep(120)
                continue

            if response.status_code != 200:
                logger.error("API returned %s: %s", response.status_code, response.text)
                last_error = f"HTTP {response.status_code}"
                time.sleep(10)
                continue

            data = response.json()
            logger.info("Got API response: %s", data.get("comment", "OK"))

            download_url = data.get("file") or data.get("reserved_file")
            if not download_url:
                raise ValueError(f"No download URL in response: {data}")

            max_wait = 300
            for wait_attempt in range(max_wait // 3):
                try:
                    head = requests.head(download_url, timeout=10, allow_redirects=True)
                    if head.status_code == 200:
                        logger.info("File ready at attempt %s", wait_attempt)
                        break
                    if head.status_code == 404:
                        logger.debug("[%ss] File still preparing (404)...", wait_attempt * 3)
                    else:
                        logger.debug("[%ss] HTTP %s", wait_attempt * 3, head.status_code)
                    time.sleep(3)
                except Exception:
                    logger.debug("[%ss] Poll failed, retry...", wait_attempt * 3)
                    time.sleep(3)
                    continue
            else:
                raise TimeoutError(f"File not ready after {max_wait}s")

            logger.info("Downloading from %s", download_url)
            dl_response = requests.get(download_url, stream=True, timeout=120, allow_redirects=True)
            if dl_response.status_code != 200:
                raise ValueError(f"Download failed: HTTP {dl_response.status_code}")

            final_path = output_path if output_path.endswith(".mp3") else f"{output_path}.mp3"
            with open(final_path, "wb") as file:
                for chunk in dl_response.iter_content(chunk_size=16384):
                    if chunk:
                        file.write(chunk)

            logger.info("Downloaded successfully: %s", final_path)
            session.close()
            return final_path

        except Exception as exc:
            last_error = str(exc)
            logger.error("Error (attempt %s): %s", attempt + 1, exc)
            time.sleep(10)
            continue

    session.close()
    raise RuntimeError(f"All keys failed. Last error: {last_error}")
