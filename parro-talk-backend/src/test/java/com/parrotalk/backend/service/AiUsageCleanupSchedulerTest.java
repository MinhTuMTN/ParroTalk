package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.parrotalk.backend.repository.AiUsageEventRepository;

class AiUsageCleanupSchedulerTest {

    @Test
    void deletesEventsOlderThanRetention() {
        AiUsageEventRepository repo = mock(AiUsageEventRepository.class);
        when(repo.deleteByCreatedAtBefore(any())).thenReturn(7);
        AiUsageCleanupScheduler s = new AiUsageCleanupScheduler(repo);
        ReflectionTestUtils.setField(s, "retentionDays", 10);
        assertEquals(7, s.cleanupOldEvents());
        ArgumentCaptor<Instant> c = ArgumentCaptor.forClass(Instant.class);
        verify(repo).deleteByCreatedAtBefore(c.capture());
        Duration age = Duration.between(c.getValue(), Instant.now());
        assertTrue(age.toDays() == 10 || age.toDays() == 9 && age.toHours() >= 239);
    }
}
