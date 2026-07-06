package com.parrotalk.backend.dto;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Wrapper class for ChatClient to store model name and masked API key.
 * 
 * @param chatClient   Chat client for the model
 * @param modelName    Name of the model
 * @param maskedApiKey Masked API key for the model
 */
public record ChatModelNode(
        ChatClient chatClient,
        String modelName,
        String maskedApiKey) {
}
