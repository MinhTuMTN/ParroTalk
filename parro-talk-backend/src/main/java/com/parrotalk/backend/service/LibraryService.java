package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.LibrarySectionDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.SectionLessonProjection;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Library Service.
 * 
 * @author MinhTuMTN
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    public static final int MAX_ITEMS_PER_SECTION = 10;

    /** Library Section Repository */
    private final LibrarySectionRepository librarySectionRepository;

    /** Lesson Repository */
    private final LessonRepository lessonRepository;

    /** Category Repository */
    private final CategoryRepository categoryRepository;

    /** Library Service (self-reference for caching methods) */
    @Lazy
    @Autowired
    private LibraryService self;

    /** Dedicated executor for DB queries */
    @Autowired
    @Qualifier("dbQueryExecutor")
    private Executor dbQueryExecutor;

    public List<LibrarySectionDTO> getLibrarySections(UUID userId) {
        // Step 1: Get all sections in DB (cached)
        List<LibrarySection> allSections = self.getAllSectionsCached();

        if (userId == null) {
            // Step 2: Get all lessons by library sections (Public sections)
            return self.getGuestLibrarySections(allSections);
        }

        // Step 3: Fetch user-specific sections (USER_PROCESSING) in parallel
        CompletableFuture<List<LibrarySectionDTO>> userSectionsFuture = CompletableFuture.supplyAsync(
                () -> self.getUserProcessingSections(userId, allSections),
                dbQueryExecutor
        );

        // Fetch guestSections on the main thread
        List<LibrarySectionDTO> guestSections = self.getGuestLibrarySections(allSections);

        List<LibrarySectionDTO> userSections = userSectionsFuture.join();
        if (userSections.isEmpty()) {
            return guestSections;
        }

        return mergeSections(allSections, guestSections, userSections);
    }

    /**
     * Get all library sections.
     * 
     * @return List of all library sections.
     */
    @Cacheable(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'active_sections'")
    public List<LibrarySection> getAllSectionsCached() {
        return librarySectionRepository.findByIsActiveTrueOrderByOrderIndexAsc();
    }

    /**
     * Fetch user-specific sections (USER_PROCESSING)
     * by joining UserLessonProgress with Lesson directly at SQL level with limit.
     */
    @Cacheable(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'user:' + #userId")
    public List<LibrarySectionDTO> getUserProcessingSections(UUID userId, List<LibrarySection> allSections) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<LibrarySection> userSectionConfigs = allSections.stream()
                .filter(s -> s.getType() == SectionType.IN_PROGRESS)
                .toList();

        if (userSectionConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        List<SectionLessonProjection> userLessons = lessonRepository.findBatchUserLessons(userId, MAX_ITEMS_PER_SECTION);
        if (userLessons.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<LessonWithProgressDTO>> itemsBySectionKey = groupProjectionsBySection(userLessons);

        return userSectionConfigs.stream()
                .map(section -> buildLibrarySectionDTO(section, itemsBySectionKey.get(getSectionKey(section))))
                .filter(Objects::nonNull)
                .toList();
    }


    private List<LibrarySectionDTO> mergeSections(
            List<LibrarySection> allSections,
            List<LibrarySectionDTO> guestSections,
            List<LibrarySectionDTO> userSections) {

        // Gộp chung tất cả các section đã fetch được
        List<LibrarySectionDTO> combined = new ArrayList<>(guestSections);
        combined.addAll(userSections);

        Map<UUID, LibrarySectionDTO> dtoMap = combined.stream()
                .collect(Collectors.toMap(LibrarySectionDTO::getId, Function.identity()));

        // Duyệt theo danh sách gốc để giữ đúng thứ tự (orderIndex)
        return allSections.stream()
                .map(section -> dtoMap.get(section.getId()))
                .filter(dto -> dto != null && dto.getItems() != null && !dto.getItems().isEmpty())
                .toList();
    }

    @Cacheable(value = CacheNames.LIBRARY_SECTIONS_CACHE, key = "'guest'")
    public List<LibrarySectionDTO> getGuestLibrarySections(List<LibrarySection> sections) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> categoryIds = new ArrayList<>();
        List<String> programs = new ArrayList<>();

        for (LibrarySection section : sections) {
            if (section.getType() == SectionType.CATEGORY
                    && section.getCategory() != null) {
                categoryIds.add(section.getCategory().getId());
            } else if (section.getType() == SectionType.PROGRAM
                    && section.getProgram() != null
                    && !section.getProgram().isBlank()) {
                programs.add(section.getProgram());
            }
        }
        CompletableFuture<List<SectionLessonProjection>> catFuture = categoryIds.isEmpty()
                ? CompletableFuture.completedFuture(Collections.emptyList())
                : CompletableFuture.supplyAsync(() -> lessonRepository.findBatchCategoryLessons(categoryIds, MAX_ITEMS_PER_SECTION), dbQueryExecutor);

        CompletableFuture<List<SectionLessonProjection>> progFuture = programs.isEmpty()
                ? CompletableFuture.completedFuture(Collections.emptyList())
                : CompletableFuture.supplyAsync(() -> lessonRepository.findBatchProgramLessons(programs, MAX_ITEMS_PER_SECTION), dbQueryExecutor);

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
            case USER_PROCESSING -> "USER_PROCESSING";
            case USER_UPLOADED -> "USER_UPLOADED";
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

    private LibrarySectionDTO buildLibrarySectionDTO(LibrarySection section, List<LessonWithProgressDTO> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return LibrarySectionDTO.builder()
                .id(section.getId())
                .title(section.getTitle())
                .type(section.getType())
                .categoryId(section.getCategory() != null ? section.getCategory().getId().toString() : null)
                .categoryName(section.getCategory() != null ? section.getCategory().getName() : null)
                .categorySlug(section.getCategory() != null ? section.getCategory().getSlug() : null)
                .program(section.getProgram())
                .items(items)
                .build();
    }

    public PageResponse<LessonWithProgressDTO> getCategoryLessons(
            String categorySlug,
            String program,
            String source,
            String sort,
            int page,
            int size,
            User user) {
        if (!categoryRepository.existsBySlug(categorySlug)) {
            throw new ParroTalkException("Category not found with slug: " + categorySlug, HttpStatus.NOT_FOUND);
        }

        UUID userId = user != null ? user.getId() : null;
        int pageIndex = Math.max(0, page);
        int pageSize = size > 0 ? size : 12;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, resolveSort(sort));

        String programFilter = (program != null && !program.isBlank()) ? program.trim() : null;
        String sourceFilter = (source != null && !source.isBlank()) ? source.trim() : null;

        Page<LessonWithProgressDTO> lessonPage = lessonRepository.findCategoryLessonsBySlug(
                categorySlug,
                programFilter,
                sourceFilter,
                userId,
                pageable);

        return mapToPageResponse(lessonPage);
    }

    public PageResponse<LessonWithProgressDTO> searchPublicLessons(
            String query,
            String category,
            String program,
            String source,
            String sort,
            int page,
            int size,
            User user) {
        UUID userId = user != null ? user.getId() : null;
        int pageIndex = Math.max(0, page);
        int pageSize = size > 0 ? size : 12;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, resolveSort(sort));

        String queryFilter = (query != null && !query.isBlank()) ? query.trim() : null;
        String categoryFilter = (category != null && !category.isBlank()) ? category.trim() : null;
        String programFilter = (program != null && !program.isBlank()) ? program.trim() : null;
        String sourceFilter = (source != null && !source.isBlank()) ? source.trim() : null;

        Page<LessonWithProgressDTO> lessonPage = lessonRepository.searchPublicLessons(
                queryFilter,
                categoryFilter,
                programFilter,
                sourceFilter,
                userId,
                pageable);

        return mapToPageResponse(lessonPage);
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank() || "latest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("duration".equalsIgnoreCase(sort) || "duration_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "duration");
        }
        if ("duration_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "duration");
        }
        if ("title".equalsIgnoreCase(sort) || "title_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "title");
        }
        if ("popular".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private <T> PageResponse<T> mapToPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
