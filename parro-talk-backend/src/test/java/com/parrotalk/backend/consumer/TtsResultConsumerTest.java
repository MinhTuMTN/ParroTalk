package com.parrotalk.backend.consumer;

import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TtsResultConsumerTest {

    @Mock
    private TtsAudioCacheRepository ttsAudioCacheRepository;

    @InjectMocks
    private TtsResultConsumer consumer;

    @Test
    @DisplayName("receiveTtsResults persists valid HTTPS Cloudinary entries and skips invalid rows")
    @SuppressWarnings("unchecked")
    void testReceiveTtsResults_PersistsValidAndSkipsInvalid() {
        String payload = """
                {
                  "results": [
                    {
                      "cacheKey": "us_abc123",
                      "cloudinaryUrl": "https://res.cloudinary.com/demo/video/upload/tts/us_abc123.mp3",
                      "accent": "US",
                      "voice": "en-US-JennyNeural"
                    },
                    {
                      "cacheKey": "   ",
                      "cloudinaryUrl": "https://res.cloudinary.com/demo/video/upload/tts/blank.mp3",
                      "accent": "US",
                      "voice": "en-US-JennyNeural"
                    },
                    {
                      "cacheKey": "uk_badurl",
                      "cloudinaryUrl": "javascript:alert(1)",
                      "accent": "UK",
                      "voice": "en-GB-SoniaNeural"
                    }
                  ]
                }
                """;
        Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), new MessageProperties());

        consumer.receiveTtsResults(message);

        ArgumentCaptor<List<TtsAudioCache>> captor = ArgumentCaptor.forClass(List.class);
        verify(ttsAudioCacheRepository, times(1)).saveAll(captor.capture());

        List<TtsAudioCache> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals("us_abc123", saved.get(0).getCacheKey());
        assertEquals("https://res.cloudinary.com/demo/video/upload/tts/us_abc123.mp3", saved.get(0).getCloudinaryUrl());
        assertEquals("US", saved.get(0).getAccent());
        assertEquals("en-US-JennyNeural", saved.get(0).getVoice());
    }

    @Test
    @DisplayName("receiveTtsResults ignores null or empty results without touching repository")
    void testReceiveTtsResults_IgnoresEmptyResults() {
        consumer.receiveTtsResults(null);
        Message emptyResults = new Message("{\"results\":[]}".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        consumer.receiveTtsResults(emptyResults);

        verifyNoInteractions(ttsAudioCacheRepository);
    }
}
