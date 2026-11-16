from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
import json
import logging
import re
import threading
import time
import pika

from app.config import (
    RABBITMQ_HOST,
    RABBITMQ_PASS,
    RABBITMQ_PORT,
    RABBITMQ_USER,
    VOCAB_CRAWL_DLQ,
    VOCAB_CRAWL_QUEUE,
    VOCAB_CRAWL_RESULT_QUEUE,
)
from app.crawler_service import CrawlService
from app.models import CrawlJobResult, MergedCrawlPayload

logger = logging.getLogger(__name__)
crawl_service = CrawlService()
_crawl_executor = ThreadPoolExecutor(max_workers=4, thread_name_prefix="vocab-crawl-exec")
_INVALID_WORD_RE = re.compile(r"[/\\\x00-\x1f\x7f]")


def _is_valid_word(word: str) -> bool:
    return bool(word) and len(word) <= 100 and not _INVALID_WORD_RE.search(word)


def _publish_result(ch, out_payload: dict) -> None:
    ch.basic_publish(
        exchange="",
        routing_key=VOCAB_CRAWL_RESULT_QUEUE,
        body=json.dumps(out_payload, ensure_ascii=False),
        properties=pika.BasicProperties(delivery_mode=pika.DeliveryMode.Persistent),
    )


def _publish_failed_result(
    ch,
    job_id: str | None,
    list_id: str | None,
    list_item_id: str | None,
    user_id: str | None,
    word: str,
    error: str,
) -> None:
    failed = CrawlJobResult(
        word=word,
        status="FAILED",
        data_source="NOT_FOUND",
        error=error,
        payload=MergedCrawlPayload(word=word),
    )
    _publish_result(
        ch,
        failed.to_message_dict(
            job_id=job_id,
            list_id=list_id,
            list_item_id=list_item_id,
            user_id=user_id,
        ),
    )


def process_crawl_message(ch, method, properties, body: bytes) -> None:
    job_id = list_id = list_item_id = user_id = None
    word = ""
    try:
        msg = json.loads(body.decode("utf-8"))
        job_id = msg.get("jobId")
        list_id = msg.get("listId")
        list_item_id = msg.get("listItemId")
        user_id = msg.get("userId")
        word = str(msg.get("word", "")).strip()

        if not _is_valid_word(word):
            logger.error("Invalid crawl message (word='%s'): %s", word, msg)
            _publish_failed_result(ch, job_id, list_id, list_item_id, user_id, word, "Invalid or blank word")
            if getattr(ch, "is_open", True):
                ch.basic_ack(delivery_tag=method.delivery_tag)
            return

        logger.info("Crawling vocabulary '%s' for listItemId=%s", word, list_item_id)
        future = _crawl_executor.submit(crawl_service.crawl_word, word)
        conn = getattr(ch, "connection", None)
        while not future.done():
            if conn is not None and hasattr(conn, "process_data_events"):
                conn.process_data_events(time_limit=1)
            else:
                time.sleep(0.05)

        result = future.result()
        out_payload = result.to_message_dict(
            job_id=job_id,
            list_id=list_id,
            list_item_id=list_item_id,
            user_id=user_id,
        )

        _publish_result(ch, out_payload)
        if getattr(ch, "is_open", True):
            ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info(
            "Published crawl result for '%s' (status=%s, source=%s)",
            word,
            result.status,
            result.data_source,
        )
    except Exception as exc:
        logger.exception("Error processing vocab crawl message: %s", exc)
        try:
            if getattr(ch, "is_open", True):
                _publish_failed_result(ch, job_id, list_id, list_item_id, user_id, word, str(exc))
                ch.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as pub_exc:
            logger.exception("Failed to publish FAILED crawl result: %s", pub_exc)
            if getattr(ch, "is_open", False):
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def start_rabbitmq_worker_loop() -> None:
    while True:
        try:
            credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=RABBITMQ_HOST,
                    port=RABBITMQ_PORT,
                    credentials=credentials,
                    heartbeat=60,
                )
            )
            channel = connection.channel()
            channel.queue_declare(
                queue=VOCAB_CRAWL_QUEUE,
                durable=True,
                arguments={
                    "x-dead-letter-exchange": "",
                    "x-dead-letter-routing-key": VOCAB_CRAWL_DLQ,
                },
            )
            channel.queue_declare(queue=VOCAB_CRAWL_RESULT_QUEUE, durable=True)
            channel.queue_declare(queue=VOCAB_CRAWL_DLQ, durable=True)
            channel.basic_qos(prefetch_count=2)
            channel.basic_consume(
                queue=VOCAB_CRAWL_QUEUE,
                on_message_callback=process_crawl_message,
            )
            logger.info("Vocab Crawler Worker listening on queue: %s", VOCAB_CRAWL_QUEUE)
            channel.start_consuming()
        except Exception as exc:
            logger.warning("RabbitMQ worker connection interrupted (%s); retrying in 5s...", exc)
            time.sleep(5)


def launch_worker_daemon() -> threading.Thread:
    thread = threading.Thread(target=start_rabbitmq_worker_loop, daemon=True, name="vocab-crawl-worker")
    thread.start()
    return thread

