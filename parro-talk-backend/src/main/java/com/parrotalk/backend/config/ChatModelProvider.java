package com.parrotalk.backend.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import com.openai.models.ReasoningEffort;
import com.parrotalk.backend.dto.ChatModelNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChatModelProvider {

    private final List<ChatModelNode> nodes;
    private final AtomicInteger current = new AtomicInteger();

    public ChatModelProvider(OpenAIProperties properties) {
        this.nodes = new ArrayList<>();
        for (String apiKey : properties.getApiKeys()) {
            for (String modelName : properties.getModel()) {
                this.nodes.add(new ChatModelNode(
                        ChatClient.builder(createChatModel(properties, apiKey, modelName)).build(),
                        modelName,
                        maskedApiKey(apiKey)));
            }
        }
    }

    private OpenAiChatModel createChatModel(OpenAIProperties properties, String apiKey, String modelName) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .apiKey(apiKey)
                .baseUrl(properties.getBaseUrl())
                .model(modelName)
                .temperature(0.1);

        if ("qwen/qwen3.6-27b".equals(modelName)) {
            options.reasoningEffort(ReasoningEffort.NONE.asString());
        }

        return OpenAiChatModel.builder()
                .options(options.build())
                .build();
    }

    private String maskedApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4)
            return apiKey;
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    public ChatModelNode current() {
        return nodes.get(current.get());
    }

    public ChatModelNode next() {
        int index = current.updateAndGet(i -> Math.floorMod(i + 1, nodes.size()));
        return nodes.get(index);
    }

    public int size() {
        return nodes.size();
    }

}
