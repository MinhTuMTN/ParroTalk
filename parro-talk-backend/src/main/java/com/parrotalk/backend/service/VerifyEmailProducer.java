package com.parrotalk.backend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.dto.VerifyEmailMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailProducer {

    private final RabbitTemplate rabbitTemplate;

    public void enqueue(VerifyEmailMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.VERIFY_EMAIL_QUEUE,
                message);
        log.info("Queued verification email for {}", message.email());
    }
}
