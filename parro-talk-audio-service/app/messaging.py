import json

from app.config import RESULT_QUEUE
from app.logging_config import logger


def send_progress(ch, lesson_id, progress, step):
    payload = {
        "lessonId": lesson_id,
        "status": "PROGRESS",
        "progress": progress,
        "message": step,
    }
    logger.info("Sending progress for lessonId: %s, progress: %s, step: %s", lesson_id, progress, step)
    ch.basic_publish(exchange="", routing_key=RESULT_QUEUE, body=json.dumps(payload))
