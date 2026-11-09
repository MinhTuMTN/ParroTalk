package com.parrotalk.backend.service;

import com.parrotalk.backend.config.RabbitMQConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabCrawlTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public UUID sendCrawlTask(UUID listId, UUID listItemId, UUID userId, String word) {
        UUID jobId = UUID.randomUUID();
        Map<String, Object> message = new HashMap<>();
        message.put("jobId", jobId.toString());
        message.put("listId", listId != null ? listId.toString() : "");
        message.put("listItemId", listItemId != null ? listItemId.toString() : "");
        message.put("userId", userId != null ? userId.toString() : "");
        message.put("word", word);

        log.info("Sending vocabulary crawl task to RabbitMQ: word='{}', listItemId={}", word, listItemId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.VOCAB_CRAWL_QUEUE, message);
        return jobId;
    }
}

