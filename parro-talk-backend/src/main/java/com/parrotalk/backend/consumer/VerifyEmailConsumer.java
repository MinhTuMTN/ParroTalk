package com.parrotalk.backend.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.dto.VerifyEmailMessage;
import com.parrotalk.backend.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final EmailService emailService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.VERIFY_EMAIL_QUEUE)
    public void consume(VerifyEmailMessage message) {
        try {
            emailService.sendVerificationEmail(message);
            log.info("Sent verification email to {}", message.email());
        } catch (Exception e) {
            if (message.attempt() < MAX_ATTEMPTS) {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.VERIFY_EMAIL_RETRY_QUEUE,
                        message.nextAttempt());
                log.warn("Verification email send failed for {}, retrying attempt {}", message.email(),
                        message.attempt() + 1);
                return;
            }

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.VERIFY_EMAIL_DLQ,
                    message);
            log.error("Verification email send failed permanently for {}", message.email(), e);
        }
    }
}
