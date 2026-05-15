import json

import pika

from app.config import PROCESSING_QUEUE, RABBITMQ_HOST, RABBITMQ_PASS, RABBITMQ_USER, RESULT_QUEUE
from app.logging_config import logger
from app.service import process_audio


def callback(ch, method, properties, body):
    try:
        msg = json.loads(body.decode("utf-8"))
        lesson_id = msg.get("lessonId")
        file_url = msg.get("fileUrl")

        if not lesson_id or not file_url:
            logger.error("Invalid message format: %s", msg)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            return

        logger.info("Received task for lessonId: %s", lesson_id)
        result_payload = process_audio(ch, lesson_id, file_url)
        result_payload["lessonId"] = lesson_id

        ch.basic_publish(
            exchange="",
            routing_key=RESULT_QUEUE,
            body=json.dumps(result_payload),
            properties=pika.BasicProperties(delivery_mode=pika.DeliveryMode.Persistent),
        )
        logger.info("Published result for lessonId: %s", lesson_id)

        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exc:
        logger.error("Failed to process message: %s", exc)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def main():
    try:
        logger.info("Connecting to RabbitMQ...")
        logger.info("RABBITMQ_HOST: %s", RABBITMQ_HOST)
        logger.info("RABBITMQ_USER: %s", RABBITMQ_USER)

        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=RABBITMQ_HOST, port=5672, credentials=credentials)
        )

        if connection.is_open:
            logger.info("Connected to RabbitMQ successfully!")
        else:
            logger.error("Connection failed!")

        channel = connection.channel()
        logger.info("Channel created")

    except Exception:
        logger.exception("Failed to connect to RabbitMQ")
        return

    channel.queue_declare(queue=PROCESSING_QUEUE, durable=True, arguments={
        "x-dead-letter-exchange": "",
        "x-dead-letter-routing-key": "audio-processing-dlq",
    })
    logger.info("Queue declared: %s", PROCESSING_QUEUE)

    channel.queue_declare(queue=RESULT_QUEUE, durable=True)
    logger.info("Queue declared: %s", RESULT_QUEUE)

    channel.basic_qos(prefetch_count=1)
    logger.info("QoS set (prefetch_count=1)")

    channel.basic_consume(queue=PROCESSING_QUEUE, on_message_callback=callback)
    logger.info("Started consuming from queue: %s", PROCESSING_QUEUE)
    logger.info(" [*] Waiting for messages. To exit press CTRL+C")

    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        logger.info("Shutting down worker...")
        channel.stop_consuming()
    finally:
        connection.close()
        logger.info("Connection closed")


if __name__ == "__main__":
    main()
