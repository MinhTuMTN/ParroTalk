package com.parrotalk.backend.service;

import com.parrotalk.backend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTranscriptionTask(UUID lessonId, String fileUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("lessonId", lessonId.toString());
        message.put("fileUrl", fileUrl);

        log.info("Sending transcription task to RabbitMQ for lesson: {}", lessonId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PROCESSING_QUEUE, message);
    }
}
