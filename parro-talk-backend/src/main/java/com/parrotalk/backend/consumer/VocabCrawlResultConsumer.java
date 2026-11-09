package com.parrotalk.backend.consumer;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.service.VocabularyEnrichmentAiService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabCrawlResultConsumer {

    private final VocabularyEnrichmentAiService vocabularyEnrichmentAiService;

    @RabbitListener(queues = RabbitMQConfig.VOCAB_CRAWL_RESULT_QUEUE)
    public void receiveCrawlResult(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received vocabulary crawl result message from RabbitMQ");
        vocabularyEnrichmentAiService.processCrawlResultMessage(body);
    }
}

