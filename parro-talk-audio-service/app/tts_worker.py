"""Dedicated RabbitMQ worker for Edge-TTS synthesis and Cloudinary upload."""

from __future__ import annotations

import json
import time

import pika

from app.config import (
    RABBITMQ_HOST,
    RABBITMQ_PASS,
    RABBITMQ_USER,
    TTS_CONCURRENCY,
    TTS_DLQ,
    TTS_RESULT_QUEUE,
    TTS_SYNTHESIS_QUEUE,
)
from app.logging_config import logger
from app.tts import synthesize_and_upload_batch


def _extract_items(payload: object) -> list[dict]:
    if not isinstance(payload, dict):
        return []
    raw_items = payload.get("items")
    if isinstance(raw_items, list):
        return [item for item in raw_items if isinstance(item, dict)]
    if isinstance(payload.get("text"), str):
        return [payload]
    return []


def callback(ch, method, properties, body) -> None:
    """Process a batch or single TTS synthesis request and publish Cloudinary URLs."""
    try:
        payload = json.loads(body.decode("utf-8"))
        items = _extract_items(payload)
        if not items:
            logger.warning("Ignoring empty/invalid TTS message: %s", payload)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            return

        logger.info("Processing TTS batch of %d item(s)", len(items))
        results = synthesize_and_upload_batch(items, concurrency=TTS_CONCURRENCY)

        if results:
            result_payload = {"results": results}
            ch.basic_publish(
                exchange="",
                routing_key=TTS_RESULT_QUEUE,
                body=json.dumps(result_payload),
                properties=pika.BasicProperties(
                    content_type="application/json",
                    delivery_mode=pika.DeliveryMode.Persistent,
                ),
            )
            logger.info("Published %d TTS Cloudinary URL(s) to %s", len(results), TTS_RESULT_QUEUE)

        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exc:
        logger.error("Failed to process TTS message: %s", exc)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def _declare_queues(channel) -> None:
    channel.queue_declare(queue=TTS_DLQ, durable=True)
    channel.queue_declare(
        queue=TTS_SYNTHESIS_QUEUE,
        durable=True,
        arguments={
            "x-dead-letter-exchange": "",
            "x-dead-letter-routing-key": TTS_DLQ,
        },
    )
    channel.queue_declare(queue=TTS_RESULT_QUEUE, durable=True)


def main() -> None:
    """Run the dedicated TTS RabbitMQ consumer loop with automatic reconnect."""
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    params = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=5672,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=300,
    )

    while True:
        connection = None
        try:
            logger.info("Connecting TTS worker to RabbitMQ (%s)...", RABBITMQ_HOST)
            connection = pika.BlockingConnection(params)
            channel = connection.channel()
            _declare_queues(channel)
            channel.basic_qos(prefetch_count=2)
            channel.basic_consume(queue=TTS_SYNTHESIS_QUEUE, on_message_callback=callback)
            logger.info("TTS worker consuming %s (concurrency=%d)", TTS_SYNTHESIS_QUEUE, TTS_CONCURRENCY)
            channel.start_consuming()
        except KeyboardInterrupt:
            logger.info("Shutting down TTS worker...")
            if connection and connection.is_open:
                connection.close()
            break
        except Exception as exc:
            logger.exception("TTS worker connection error: %s; reconnecting in 5s", exc)
            if connection and connection.is_open:
                try:
                    connection.close()
                except Exception:
                    pass
            time.sleep(5)


if __name__ == "__main__":
    main()
