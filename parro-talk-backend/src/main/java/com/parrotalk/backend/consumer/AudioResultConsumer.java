package com.parrotalk.backend.consumer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.service.AudioProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Consume audio result from RabbitMQ.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AudioResultConsumer {

    /** Audio processing service */
    private final AudioProcessingService audioProcessingService;

    /** Object Mapper (get JSON data from message) */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Receive message from RabbitMQ.
     * Queue name: {@link RabbitMQConfig.RESULT_QUEUE}
     * 
     * @param message Message
     */
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void receiveResult(Message message) {
        JsonNode node = mapper.readTree(message.getBody());
        log.info("Received message from {}, data: {}", RabbitMQConfig.RESULT_QUEUE, node);
        audioProcessingService.process(node);
    }
}
