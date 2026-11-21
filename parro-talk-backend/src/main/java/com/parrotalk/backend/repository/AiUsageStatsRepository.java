package com.parrotalk.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parrotalk.backend.entity.AiUsageEvent;

/**
 * Read-only aggregate queries over {@code ai_usage_events} (native PostgreSQL).
 *
 * <p>Filter params are all optional: {@code userId} is a text UUID (null = any),
 * {@code systemOnly} restricts to {@code user_id IS NULL}.
 */
public interface AiUsageStatsRepository extends org.springframework.data.repository.Repository<AiUsageEvent, UUID> {

    String FILTER = """
            e.created_at >= :from AND e.created_at < :to
            AND (CAST(:userId AS text) IS NULL OR CAST(e.user_id AS text) = CAST(:userId AS text))
            AND (:systemOnly = false OR e.user_id IS NULL)
            AND (CAST(:model AS text) IS NULL OR e.model = CAST(:model AS text))
            AND (CAST(:provider AS text) IS NULL OR e.provider = CAST(:provider AS text))
            AND (CAST(:feature AS text) IS NULL OR e.feature = CAST(:feature AS text))
            """;

    interface SummaryRow {
        Long getRequests();
        Long getInputTokens();
        Long getOutputTokens();
        Long getActiveUsers();
    }

    interface DailyRow {
        Object getDay();
        Long getRequests();
        Long getInputTokens();
        Long getOutputTokens();
    }

    interface ModelRow {
        String getProvider();
        String getModel();
        Long getRequests();
        Long getInputTokens();
        Long getOutputTokens();
    }

    interface FeatureRow {
        String getFeature();
        Long getRequests();
        Long getInputTokens();
        Long getOutputTokens();
    }

    interface UserRow {
        UUID getUserId();
        String getEmail();
        String getFullName();
        String getUsername();
        Boolean getDeleted();
        Long getRequests();
        Long getInputTokens();
        Long getOutputTokens();
        Object getLastUsedAt();
    }

    @Query(value = """
            SELECT COUNT(*) AS "requests",
                   COALESCE(SUM(e.input_tokens), 0) AS "inputTokens",
                   COALESCE(SUM(e.output_tokens), 0) AS "outputTokens",
                   COUNT(DISTINCT e.user_id) AS "activeUsers"
            FROM public.ai_usage_events e
            WHERE """ + FILTER, nativeQuery = true)
    SummaryRow summary(@Param("from") Instant from, @Param("to") Instant to,
            @Param("userId") String userId, @Param("systemOnly") boolean systemOnly,
            @Param("model") String model, @Param("provider") String provider,
            @Param("feature") String feature);

    @Query(value = """
            SELECT CAST(timezone(CAST(:tz AS text), e.created_at) AS date) AS "day",
                   COUNT(*) AS "requests",
                   COALESCE(SUM(e.input_tokens), 0) AS "inputTokens",
                   COALESCE(SUM(e.output_tokens), 0) AS "outputTokens"
            FROM public.ai_usage_events e
            WHERE """ + FILTER + """
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<DailyRow> daily(@Param("from") Instant from, @Param("to") Instant to, @Param("tz") String tz,
            @Param("userId") String userId, @Param("systemOnly") boolean systemOnly,
            @Param("model") String model, @Param("provider") String provider,
            @Param("feature") String feature);

    @Query(value = """
            SELECT e.provider AS "provider", e.model AS "model",
                   COUNT(*) AS "requests",
                   COALESCE(SUM(e.input_tokens), 0) AS "inputTokens",
                   COALESCE(SUM(e.output_tokens), 0) AS "outputTokens"
            FROM public.ai_usage_events e
            WHERE """ + FILTER + """
            GROUP BY e.provider, e.model
            ORDER BY COALESCE(SUM(e.input_tokens), 0) + COALESCE(SUM(e.output_tokens), 0) DESC
            """, nativeQuery = true)
    List<ModelRow> byModel(@Param("from") Instant from, @Param("to") Instant to,
            @Param("userId") String userId, @Param("systemOnly") boolean systemOnly,
            @Param("model") String model, @Param("provider") String provider,
            @Param("feature") String feature);

    @Query(value = """
            SELECT e.feature AS "feature",
                   COUNT(*) AS "requests",
                   COALESCE(SUM(e.input_tokens), 0) AS "inputTokens",
                   COALESCE(SUM(e.output_tokens), 0) AS "outputTokens"
            FROM public.ai_usage_events e
            WHERE """ + FILTER + """
            GROUP BY e.feature
            ORDER BY COALESCE(SUM(e.input_tokens), 0) + COALESCE(SUM(e.output_tokens), 0) DESC
            """, nativeQuery = true)
    List<FeatureRow> byFeature(@Param("from") Instant from, @Param("to") Instant to,
            @Param("userId") String userId, @Param("systemOnly") boolean systemOnly,
            @Param("model") String model, @Param("provider") String provider,
            @Param("feature") String feature);

    /** Includes soft-deleted users (native join bypasses the entity @SQLRestriction). */
    @Query(value = """
            SELECT e.user_id AS "userId", u.email AS "email", u.full_name AS "fullName",
                   u.username AS "username", u.is_deleted AS "deleted",
                   COUNT(*) AS "requests",
                   COALESCE(SUM(e.input_tokens), 0) AS "inputTokens",
                   COALESCE(SUM(e.output_tokens), 0) AS "outputTokens",
                   MAX(e.created_at) AS "lastUsedAt"
            FROM public.ai_usage_events e
            LEFT JOIN public.users u ON u.id = e.user_id
            WHERE """ + FILTER + """
              AND (CAST(:search AS text) IS NULL
                   OR LOWER(u.email) LIKE CAST(:search AS text) ESCAPE '\\'
                   OR LOWER(u.full_name) LIKE CAST(:search AS text) ESCAPE '\\'
                   OR LOWER(u.username) LIKE CAST(:search AS text) ESCAPE '\\')
            GROUP BY e.user_id, u.email, u.full_name, u.username, u.is_deleted
            """, nativeQuery = true)
    List<UserRow> byUser(@Param("from") Instant from, @Param("to") Instant to,
            @Param("userId") String userId, @Param("systemOnly") boolean systemOnly,
            @Param("model") String model, @Param("provider") String provider,
            @Param("feature") String feature, @Param("search") String search);
}
