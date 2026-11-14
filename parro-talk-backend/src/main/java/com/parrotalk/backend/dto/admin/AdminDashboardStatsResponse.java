package com.parrotalk.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * High-level aggregated statistics for Admin Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalLessons;
    private long publishedLessons;
    private long hiddenLessons;
    private long totalCategories;
    private long totalTags;
    private long totalVocabularies;
    private long pendingLessonReports;
    private long pendingFeedbacks;
    private List<RecentActivityItem> recentLessons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private String id;
        private String title;
        private String status;
        private String createdAt;
        private int totalSegments;
    }
}
