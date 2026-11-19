package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.parrotalk.backend.config.CloudflareProperties;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.LibrarySectionDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Library Service.
 * 
 * @author MinhTuMTN
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class LibraryService {

    /** Lesson Repository */
    private final LessonRepository lessonRepository;

    /** Category Repository */
    private final CategoryRepository categoryRepository;

    /** Library Cache Service */
    private final LibraryCacheService libraryCacheService;

    /** Dedicated executor for DB queries */
    private final Executor dbQueryExecutor;

    /** Cloudflare embedding client */
    private final CloudflareEmbeddingClient cloudflareEmbeddingClient;

    /** Cloudflare properties */
    private final CloudflareProperties cloudflareProperties;

    /**
     * Constructor.
     */
    public LibraryService(
            LessonRepository lessonRepository,
            CategoryRepository categoryRepository,
            LibraryCacheService libraryCacheService,
            @Qualifier("dbQueryExecutor") Executor dbQueryExecutor,
            CloudflareEmbeddingClient cloudflareEmbeddingClient,
            CloudflareProperties cloudflareProperties) {
        this.lessonRepository = lessonRepository;
        this.categoryRepository = categoryRepository;
        this.libraryCacheService = libraryCacheService;
        this.dbQueryExecutor = dbQueryExecutor;
        this.cloudflareEmbeddingClient = cloudflareEmbeddingClient;
        this.cloudflareProperties = cloudflareProperties;
    }

    /**
     * Get library sections.
     * <br> (Using in library page, fetch all sections and their lessons in one go)
     * 
     * @param user User
     * @return List of library sections
     */
    public List<LibrarySectionDTO> getLibrarySections(User user) {
        // Step 1: Get all sections in DB (cached)
        List<LibrarySection> allSections = libraryCacheService.getAllSectionsCached();

        Set<SectionType> activeTypes = allSections.stream()
                .map(LibrarySection::getType)
                .collect(Collectors.toSet());

        // Step 2: Fetch user-specific sections in parallel only when authenticated and IN_PROGRESS is active
        CompletableFuture<List<LibrarySectionDTO>> userSectionsFuture =
                (user != null && activeTypes.contains(SectionType.IN_PROGRESS))
                        ? CompletableFuture.supplyAsync(
                                () -> libraryCacheService.getUserProcessingSections(user, allSections),
                                dbQueryExecutor
                        )
                        : CompletableFuture.completedFuture(Collections.emptyList());

        // Step 3: Fetch guestSections on the main thread
        List<LibrarySectionDTO> guestSections = libraryCacheService.getGuestLibrarySections(allSections);

        List<LibrarySectionDTO> userSections = userSectionsFuture.join();
        if (userSections.isEmpty()) {
            return guestSections;
        }

        return mergeSections(allSections, guestSections, userSections);
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

    /**
     * Legacy LIKE-based public lesson search.
     *
     * @deprecated Use {@link #searchPublicLessonsSemantic} instead.
     */
    @Deprecated(since = "2026-11-19", forRemoval = false)
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

    /**
     * Semantic search over vectorized public lessons with automatic fallback to LIKE search.
     */
    public PageResponse<LessonWithProgressDTO> searchPublicLessonsSemantic(
            String query,
            String category,
            String program,
            String source,
            String sort,
            int page,
            int size,
            User user) {
        String queryFilter = (query != null && !query.isBlank()) ? query.trim() : null;
        if (queryFilter == null) {
            return searchPublicLessons(null, category, program, source, sort, page, size, user);
        }

        Optional<String> queryVectorOpt = cloudflareEmbeddingClient.embedQueryToVectorLiteral(queryFilter);
        if (queryVectorOpt.isEmpty()) {
            log.debug("Semantic embedding unavailable for query '{}', falling back to LIKE search", queryFilter);
            return searchPublicLessons(queryFilter, category, program, source, sort, page, size, user);
        }

        UUID userId = user != null ? user.getId() : null;
        int pageIndex = Math.max(0, page);
        int pageSize = size > 0 ? size : 12;
        String categoryFilter = (category != null && !category.isBlank()) ? category.trim() : "";
        String programFilter = (program != null && !program.isBlank()) ? program.trim() : "";
        String sourceFilter = (source != null && !source.isBlank()) ? source.trim() : "";
        String sortKey = resolveSemanticSortKey(sort);

        Page<Object[]> rawPage = lessonRepository.searchPublicLessonsSemantic(
                queryVectorOpt.get(),
                cloudflareProperties.getMinSimilarity(),
                categoryFilter,
                programFilter,
                sourceFilter,
                userId,
                sortKey,
                PageRequest.of(pageIndex, pageSize));

        List<LessonWithProgressDTO> content = rawPage.getContent().stream()
                .map(this::mapSemanticRow)
                .toList();

        return PageResponse.<LessonWithProgressDTO>builder()
                .content(content)
                .page(rawPage.getNumber())
                .size(rawPage.getSize())
                .totalElements(rawPage.getTotalElements())
                .totalPages(rawPage.getTotalPages())
                .build();
    }

    private String resolveSemanticSortKey(String sort) {
        if (sort == null || sort.isBlank() || "relevance".equalsIgnoreCase(sort)) {
            return "relevance";
        }
        String normalized = sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "duration", "duration_asc" -> "duration_asc";
            case "duration_desc" -> "duration_desc";
            case "title", "title_asc" -> "title_asc";
            case "popular" -> "popular";
            case "latest" -> "latest";
            default -> "relevance";
        };
    }

    private LessonWithProgressDTO mapSemanticRow(Object[] row) {
        UUID id = row[0] instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(row[0]));
        String title = row[1] != null ? String.valueOf(row[1]) : null;
        double progress = row[2] instanceof Number num ? num.doubleValue() : 0.0;
        String thumbnailUrl = row[3] != null ? String.valueOf(row[3]) : null;
        String fileUrl = row[4] != null ? String.valueOf(row[4]) : null;
        Integer duration = row[5] instanceof Number num ? num.intValue() : null;
        return new LessonWithProgressDTO(id, title, progress, thumbnailUrl, fileUrl, duration);
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank() || "latest".equalsIgnoreCase(sort) || "relevance".equalsIgnoreCase(sort)) {
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
