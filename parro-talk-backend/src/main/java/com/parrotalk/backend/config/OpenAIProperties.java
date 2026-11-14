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

    /**
     * Per-request HTTP timeout (seconds). Keep it short so a stuck/slow key fails fast
     * and {@code AiCoreClient} can rotate to the next key instead of hanging.
     */
    private int timeoutSeconds = 30;

    /**
     * Retries performed silently inside the OpenAI SDK (it honours Groq's Retry-After on 429,
     * which can block a request for 10-20s+). Defaults to 0 because the application already
     * retries with key/model rotation via {@code @Retryable}.
     */
    private int sdkMaxRetries = 0;
}
