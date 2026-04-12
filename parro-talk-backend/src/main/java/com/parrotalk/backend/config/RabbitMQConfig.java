package com.parrotalk.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PROCESSING_QUEUE = "audio-processing-queue";
    public static final String RESULT_QUEUE = "audio-result-queue";
    public static final String DLQ = "audio-processing-dlq";
    public static final String EXCHANGE = "audio-exchange";

    @Bean
    public Queue processingQueue() {
        return QueueBuilder.durable(PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).build();
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding bindingProcessingQueue(Queue processingQueue, DirectExchange exchange) {
        return BindingBuilder.bind(processingQueue).to(exchange).with(PROCESSING_QUEUE);
    }

    @Bean
    public Binding bindingResultQueue(Queue resultQueue, DirectExchange exchange) {
        return BindingBuilder.bind(resultQueue).to(exchange).with(RESULT_QUEUE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
