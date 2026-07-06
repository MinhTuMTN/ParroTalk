package com.parrotalk.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "application.ai")
@Data
public class OpenAIProperties {
    private List<String> apiKeys = new ArrayList<>();
    private String baseUrl;
    private List<String> model = new ArrayList<>();
}
