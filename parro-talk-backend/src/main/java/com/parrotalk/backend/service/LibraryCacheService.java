package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.LibrarySectionDTO;
import com.parrotalk.backend.dto.SectionLessonProjection;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Library Cache Service.
 * Handles cacheable queries for library sections and their lessons.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class LibraryCacheService {

    /** Default max items per section if not specified */
    public static final int DEFAULT_MAX_ITEMS_PER_SECTION = 10;

    /** Maximum allowed items per section to prevent excessive data load */
    public static final int MAX_ALLOWED_ITEMS_PER_SECTION = 15;

    /** Library Section Repository */
    private final LibrarySectionRepository librarySectionRepository;

    /** Lesson Repository */
    private final LessonRepository lessonRepository;

    /** Dedicated executor for DB queries */
    private final Executor dbQueryExecutor;

    /**
     * Constructor.
     * 
     * @param librarySectionRepository Library Section Repository
     * @param lessonRepository Lesson Repository
     * @param dbQueryExecutor Dedicated executor for DB queries
     */
    public LibraryCacheService(
            LibrarySectionRepository librarySectionRepository,
            LessonRepository lessonRepository,
            @Qualifier("dbQueryExecutor") Executor dbQueryExecutor) {
        this.librarySectionRepository = librarySectionRepository;
        this.lessonRepository = lessonRepository;
        this.dbQueryExecutor = dbQueryExecutor;
    }

    /**
     * Get all active library sections.
     *
     * @return List of all active library sections.
     */
    @Cacheable(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'active_sections'")
    public List<LibrarySection> getAllSectionsCached() {
        return librarySectionRepository.findByIsActiveTrueOrderByOrderIndexAsc();
    }

    /**
     * Fetch user-specific sections (IN_PROGRESS)
     * by joining UserLessonProgress with Lesson directly at SQL level with limit.
     */
    @Cacheable(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'user:' + #user.id")
    public List<LibrarySectionDTO> getUserProcessingSections(User user, List<LibrarySection> allSections) {
        if (user == null) {
            return Collections.emptyList();
        }

        // Keep only IN_PROGRESS sections to skip DB query and avoid re-processing public sections.
        List<LibrarySection> userSectionConfigs = allSections.stream()
                .filter(s -> s.getType() == SectionType.IN_PROGRESS)
                .toList();

        if (userSectionConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Determine the maximum number of items to fetch for user-specific sections
        int userMaxItems = userSectionConfigs.stream()
                .mapToInt(this::resolveMaxItems)
                .max()
                .orElse(DEFAULT_MAX_ITEMS_PER_SECTION);

        // Step 2: Fetch user lessons in batch with limit
        List<SectionLessonProjection> userLessons = lessonRepository.findBatchUserLessons(user.getId(), userMaxItems);
        if (userLessons.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<LessonWithProgressDTO>> itemsBySectionKey = groupProjectionsBySection(userLessons);

        return userSectionConfigs.stream()
                .map(section -> buildLibrarySectionDTO(section, itemsBySectionKey.get(getSectionKey(section))))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Evict cached user-specific processing sections (IN_PROGRESS).
     *
     * @param userId Authenticated user id
     */
    @CacheEvict(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'user:' + #userId", condition = "#userId != null")
    public void evictUserProcessingSections(UUID userId) {
        log.debug("Evicting library sections cache for user {}", userId);
    }

    /**
     * Fetch public guest library sections (CATEGORY and PROGRAM).
     */
    @Cacheable(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'guest'")
    public List<LibrarySectionDTO> getGuestLibrarySections(List<LibrarySection> sections) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> categoryIds = new ArrayList<>();
        List<String> programs = new ArrayList<>();
        int catMaxItems = 0;
        int progMaxItems = 0;

        for (LibrarySection section : sections) {
            if (section.getType() == SectionType.CATEGORY
                    && section.getCategory() != null) {
                categoryIds.add(section.getCategory().getId());
                catMaxItems = Math.max(catMaxItems, resolveMaxItems(section));
            } else if (section.getType() == SectionType.PROGRAM
                    && section.getProgram() != null
                    && !section.getProgram().isBlank()) {
                programs.add(section.getProgram());
                progMaxItems = Math.max(progMaxItems, resolveMaxItems(section));
            }
        }

        final int batchCatMaxItems = catMaxItems;
        final int batchProgMaxItems = progMaxItems;

        CompletableFuture<List<SectionLessonProjection>> catFuture = categoryIds.isEmpty()
                ? CompletableFuture.completedFuture(Collections.emptyList())
                : CompletableFuture.supplyAsync(() -> lessonRepository.findBatchCategoryLessons(categoryIds, batchCatMaxItems), dbQueryExecutor);

        CompletableFuture<List<SectionLessonProjection>> progFuture = programs.isEmpty()
                ? CompletableFuture.completedFuture(Collections.emptyList())
                : CompletableFuture.supplyAsync(() -> lessonRepository.findBatchProgramLessons(programs, batchProgMaxItems), dbQueryExecutor);

        List<SectionLessonProjection> catLessons = catFuture.join();
        List<SectionLessonProjection> progLessons = progFuture.join();

        Map<String, List<LessonWithProgressDTO>> itemsBySectionKey = groupProjectionsBySection(
                Stream.concat(catLessons.stream(), progLessons.stream()).toList()
        );

        return sections.stream()
                .filter(s -> s.getType() == SectionType.CATEGORY || s.getType() == SectionType.PROGRAM)
                .map(section -> buildLibrarySectionDTO(section, itemsBySectionKey.get(getSectionKey(section))))
                .filter(Objects::nonNull)
                .toList();
    }

    private String getSectionKey(LibrarySection section) {
        return switch (section.getType()) {
            case CATEGORY -> section.getCategory() != null ? section.getCategory().getId().toString() : null;
            case PROGRAM -> section.getProgram();
            case IN_PROGRESS -> "IN_PROGRESS";
            default -> null;
        };
    }

    private Map<String, List<LessonWithProgressDTO>> groupProjectionsBySection(List<SectionLessonProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyMap();
        }
        return projections.stream()
                .filter(p -> p.getSectionKey() != null && p.getId() != null)
                .collect(Collectors.groupingBy(
                        SectionLessonProjection::getSectionKey,
                        Collectors.mapping(
                                p -> new LessonWithProgressDTO(
                                        UUID.fromString(p.getId()),
                                        p.getTitle(),
                                        p.getProgress() != null ? p.getProgress() : 0.0,
                                        p.getThumbnailUrl(),
                                        p.getFileUrl(),
                                        p.getDuration()
                                ),
                                Collectors.toList()
                        )
                ));
    }

    private int resolveMaxItems(LibrarySection section) {
        if (section == null || section.getMaxItems() == null || section.getMaxItems() <= 0) {
            return DEFAULT_MAX_ITEMS_PER_SECTION;
        }
        return Math.min(section.getMaxItems(), MAX_ALLOWED_ITEMS_PER_SECTION);
    }

    private LibrarySectionDTO buildLibrarySectionDTO(LibrarySection section, List<LessonWithProgressDTO> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        int limit = resolveMaxItems(section);
        List<LessonWithProgressDTO> limitedItems = items.size() > limit
                ? items.stream().limit(limit).toList()
                : items;

        return LibrarySectionDTO.builder()
                .id(section.getId())
                .title(section.getTitle())
                .type(section.getType())
                .categoryId(section.getCategory() != null ? section.getCategory().getId().toString() : null)
                .categoryName(section.getCategory() != null ? section.getCategory().getName() : null)
                .categorySlug(section.getCategory() != null ? section.getCategory().getSlug() : null)
                .program(section.getProgram())
                .items(limitedItems)
                .build();
    }
}
