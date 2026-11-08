from __future__ import annotations

import json
import logging
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

logger = logging.getLogger(__name__)
crawl_service = CrawlService()


def process_crawl_message(ch, method, properties, body: bytes) -> None:
    try:
        msg = json.loads(body.decode("utf-8"))
        job_id = msg.get("jobId")
        list_id = msg.get("listId")
        list_item_id = msg.get("listItemId")
        user_id = msg.get("userId")
        word = str(msg.get("word", "")).strip()

        if not word:
            logger.error("Invalid crawl message (blank word): %s", msg)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            return

        logger.info("Crawling vocabulary '%s' for listItemId=%s", word, list_item_id)
        result = crawl_service.crawl_word(word)
        out_payload = result.to_message_dict(
            job_id=job_id,
            list_id=list_id,
            list_item_id=list_item_id,
            user_id=user_id,
        )

        ch.basic_publish(
            exchange="",
            routing_key=VOCAB_CRAWL_RESULT_QUEUE,
            body=json.dumps(out_payload, ensure_ascii=False),
            properties=pika.BasicProperties(delivery_mode=pika.DeliveryMode.Persistent),
        )
        ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info(
            "Published crawl result for '%s' (status=%s, source=%s)",
            word,
            result.status,
            result.data_source,
        )
    except Exception as exc:
        logger.exception("Error processing vocab crawl message: %s", exc)
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

