package com.parrotalk.backend.ai;

import java.util.UUID;

/**
 * Thread-local holder of the user/feature on whose behalf AI calls are made.
 * Use with try-with-resources: {@code try (var s = AiUsageContext.open(userId, feature)) { ... }}.
 * The previous value is always restored (or cleared) on close.
 */
public final class AiUsageContext {

    public record Value(UUID userId, String feature) {
    }

    private static final ThreadLocal<Value> HOLDER = new ThreadLocal<>();

    private AiUsageContext() {
    }

    public static Scope open(UUID userId, String feature) {
        Value previous = HOLDER.get();
        HOLDER.set(new Value(userId, feature == null || feature.isBlank() ? AiFeature.UNKNOWN : feature));
        return new Scope(previous);
    }

    /**
     * Runs {@code action} tagged with {@code feature}, preserving any userId already set in the
     * current context (a feature-only scope never hides the authenticated user: the recorder
     * falls back to the SecurityContext when userId is null). Context is always restored.
     */
    public static <T> T withFeature(String feature, java.util.function.Supplier<T> action) {
        Value current = HOLDER.get();
        try (Scope ignored = open(current != null ? current.userId() : null, feature)) {
            return action.get();
        }
    }

    /** Like {@link #withFeature(String, java.util.function.Supplier)} but with an explicit user. */
    public static <T> T withFeature(UUID userId, String feature, java.util.function.Supplier<T> action) {
        Value current = HOLDER.get();
        UUID effective = userId != null ? userId : (current != null ? current.userId() : null);
        try (Scope ignored = open(effective, feature)) {
            return action.get();
        }
    }

    /** @return current context or {@code null} when none. */
    public static Value current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static final class Scope implements AutoCloseable {
        private final Value previous;

        private Scope(Value previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }
}
