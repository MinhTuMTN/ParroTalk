import os
import threading

from app.tts_worker import main as run_tts_worker
from app.worker import main as run_audio_worker


def _start_tts_worker_if_enabled() -> None:
    enabled = os.getenv("ENABLE_TTS_WORKER", "true").strip().lower() not in {"0", "false", "no", "off"}
    if not enabled:
        return
    tts_thread = threading.Thread(target=run_tts_worker, name="tts-worker", daemon=True)
    tts_thread.start()


if __name__ == "__main__":
    _start_tts_worker_if_enabled()
    run_audio_worker()
