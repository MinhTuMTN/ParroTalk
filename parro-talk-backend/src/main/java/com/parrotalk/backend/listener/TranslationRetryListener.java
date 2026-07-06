package com.parrotalk.backend.listener;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;
import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.dto.ChatModelNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component("translationRetryListener")
public class TranslationRetryListener implements RetryListener {

    private final ChatModelProvider chatModelProvider;

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
            Throwable throwable) {
        ChatModelNode current = chatModelProvider.current();
        Throwable lastThrowable = context.getLastThrowable();
        String errorMessage = lastThrowable != null ? lastThrowable.getMessage() : "Unknown error";
        log.error(
                "Failed to translate with model {}, apiKey {}, retryCount {}, error: {}",
                current.modelName(),
                current.maskedApiKey(),
                context.getRetryCount(),
                errorMessage);
        chatModelProvider.next();
    }
}
