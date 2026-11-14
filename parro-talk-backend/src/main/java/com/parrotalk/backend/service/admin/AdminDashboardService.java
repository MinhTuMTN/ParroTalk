package com.parrotalk.backend.service.admin;

import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.constant.UserStatus;
import com.parrotalk.backend.dto.admin.AdminDashboardStatsResponse;
import com.parrotalk.backend.dto.admin.AdminDashboardStatsResponse.RecentActivityItem;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final VocabularyRepository vocabularyRepository;
    private final LessonReportRepository lessonReportRepository;
    private final AppFeedbackRepository appFeedbackRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.count((root, query, cb) -> cb.equal(root.get("status"), UserStatus.ACTIVE));

        long totalLessons = lessonRepository.count();
        long publishedLessons = lessonRepository.count((root, query, cb) ->
                cb.equal(root.get("visibilityStatus"), LessonVisibilityStatus.PUBLISHED));
        long hiddenLessons = lessonRepository.count((root, query, cb) ->
                cb.equal(root.get("visibilityStatus"), LessonVisibilityStatus.HIDDEN));

        long totalCategories = categoryRepository.count();
        long totalTags = tagRepository.count();
        long totalVocabularies = vocabularyRepository.count();

        long pendingLessonReports = lessonReportRepository.count((root, query, cb) ->
                cb.equal(root.get("moderationState").get("status"), ModerationStatus.OPEN));

        long pendingFeedbacks = appFeedbackRepository.count((root, query, cb) ->
                cb.equal(root.get("moderationState").get("status"), ModerationStatus.OPEN));

        List<RecentActivityItem> recentLessons = Collections.emptyList();
        try {
            List<Lesson> topLessons = lessonRepository
                    .findAll(PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                    .getContent();

            recentLessons = topLessons.stream().map(l -> RecentActivityItem.builder()
                    .id(l.getId() != null ? l.getId().toString() : "")
                    .title(l.getTitle() != null ? l.getTitle() : "Untitled")
                    .status(l.getVisibilityStatus() != null ? l.getVisibilityStatus().name() : "HIDDEN")
                    .createdAt(l.getCreatedAt() != null ? l.getCreatedAt().toString() : "")
                    .totalSegments(l.getTotalSegments() != null ? l.getTotalSegments() : 0)
                    .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch recent lessons for dashboard: {}", e.getMessage());
        }

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalLessons(totalLessons)
                .publishedLessons(publishedLessons)
                .hiddenLessons(hiddenLessons)
                .totalCategories(totalCategories)
                .totalTags(totalTags)
                .totalVocabularies(totalVocabularies)
                .pendingLessonReports(pendingLessonReports)
                .pendingFeedbacks(pendingFeedbacks)
                .recentLessons(recentLessons)
                .build();
    }
}
