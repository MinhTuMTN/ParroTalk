package com.parrotalk.backend.consumer;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumes Cloudinary URL results produced by the Python Edge-TTS RabbitMQ worker
 * and persists them into {@code tts_audio_cache}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TtsResultConsumer {

    private final TtsAudioCacheRepository ttsAudioCacheRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.TTS_RESULT_QUEUE)
    @Transactional
    public void receiveTtsResults(Message message) {
        if (message == null || message.getBody() == null) {
            return;
        }
        JsonNode root = mapper.readTree(message.getBody());
        JsonNode resultsNode = root.path("results");
        if (!resultsNode.isArray() || resultsNode.isEmpty()) {
            return;
        }

        List<TtsAudioCache> toSave = new ArrayList<>();
        for (JsonNode item : resultsNode) {
            String cacheKey = item.path("cacheKey").asText("").trim();
            String cloudinaryUrl = item.path("cloudinaryUrl").asText("").trim();
            if (cacheKey.isEmpty() || !isAllowedHttpUrl(cloudinaryUrl)) {
                continue;
            }
            String accent = item.path("accent").asText("US").trim();
            String voice = item.path("voice").asText("en-US-JennyNeural").trim();
            toSave.add(TtsAudioCache.builder()
                    .cacheKey(cacheKey)
                    .cloudinaryUrl(cloudinaryUrl)
                    .accent(accent)
                    .voice(voice)
                    .build());
        }

        if (!toSave.isEmpty()) {
            ttsAudioCacheRepository.saveAll(toSave);
            log.debug("Saved {} Cloudinary TTS cache entries from RabbitMQ", toSave.size());
        }
    }

    private boolean isAllowedHttpUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }
}
