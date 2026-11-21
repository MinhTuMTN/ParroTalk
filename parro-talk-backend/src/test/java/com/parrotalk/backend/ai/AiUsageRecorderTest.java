package com.parrotalk.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.parrotalk.backend.entity.AiUsageEvent;
import com.parrotalk.backend.repository.AiUsageEventRepository;

class AiUsageRecorderTest {

    @Test
    @SuppressWarnings("unchecked")
    void flushPersistsQueuedEvents() {
        AiUsageEventRepository repo = mock(AiUsageEventRepository.class);
        AiUsageRecorder r = new AiUsageRecorder(repo, 100);
        UUID uid = UUID.randomUUID();
        r.record(uid, "p", "m", "F", 1, 2, "OK");
        verifyNoInteractions(repo);
        r.flush();
        ArgumentCaptor<List<AiUsageEvent>> c = ArgumentCaptor.forClass(List.class);
        verify(repo).saveAll(c.capture());
        AiUsageEvent e = c.getValue().get(0);
        assertEquals(uid, e.getUserId());
        assertEquals(1, e.getInputTokens());
        assertEquals(2, e.getOutputTokens());
        assertEquals("F", e.getFeature());
        assertNotNull(e.getId());
        assertNotNull(e.getCreatedAt());
        assertEquals(0, r.pending());
        r.shutdown();
    }

    @Test
    void dropsWhenFullAndNeverThrows() {
        AiUsageEventRepository repo = mock(AiUsageEventRepository.class);
        AiUsageRecorder r = new AiUsageRecorder(repo, 2);
        for (int i = 0; i < 5; i++) {
            r.record(null, "p", "m", null, 1, 1, null);
        }
        assertEquals(2, r.pending());
    }

    @Test
    void repositoryFailureIsSwallowed() {
        AiUsageEventRepository repo = mock(AiUsageEventRepository.class);
        when(repo.saveAll(anyList())).thenThrow(new RuntimeException("db down"));
        AiUsageRecorder r = new AiUsageRecorder(repo, 10);
        r.record(null, "p", "m", "F", 1, 1, "OK");
        assertDoesNotThrow(r::flush);
        assertEquals(0, r.pending());
    }

    @Test
    void shutdownFlushes() {
        AiUsageEventRepository repo = mock(AiUsageEventRepository.class);
        AiUsageRecorder r = new AiUsageRecorder(repo, 10);
        r.record(null, "p", "m", "F", 1, 1, "OK");
        r.shutdown();
        verify(repo).saveAll(anyList());
    }
}
