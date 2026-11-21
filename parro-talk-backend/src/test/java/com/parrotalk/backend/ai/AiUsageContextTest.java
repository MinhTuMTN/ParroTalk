package com.parrotalk.backend.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AiUsageContextTest {

    @Test
    void clearedAfterScopeEvenOnException() {
        assertNull(AiUsageContext.current());
        assertThrows(IllegalStateException.class, () -> {
            try (var s = AiUsageContext.open(UUID.randomUUID(), "F")) {
                assertEquals("F", AiUsageContext.current().feature());
                throw new IllegalStateException();
            }
        });
        assertNull(AiUsageContext.current());
    }

    @Test
    void nestedScopeRestoresPrevious() {
        UUID a = UUID.randomUUID();
        try (var s1 = AiUsageContext.open(a, "A")) {
            try (var s2 = AiUsageContext.open(null, "B")) {
                assertEquals("B", AiUsageContext.current().feature());
            }
            assertEquals(a, AiUsageContext.current().userId());
        }
        assertNull(AiUsageContext.current());
    }

    @Test
    void notVisibleOnOtherThreads() throws Exception {
        try (var s = AiUsageContext.open(UUID.randomUUID(), "A")) {
            Object[] seen = new Object[1];
            Thread t = new Thread(() -> seen[0] = AiUsageContext.current());
            t.start();
            t.join();
            assertNull(seen[0]);
        }
    }
}
