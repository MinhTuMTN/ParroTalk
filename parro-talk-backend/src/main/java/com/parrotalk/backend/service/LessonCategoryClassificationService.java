package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.LessonCategoryAnalysisResponse;
import com.parrotalk.backend.dto.LlmCategoryClassificationResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for analyzing and classifying categories for a lesson using AI.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonCategoryClassificationService {

    /** System prompt for AI classification */
    private static final String SYSTEM_PROMPT = """
                You are an expert English language educational content classifier.
                Your task is to analyze the content of an English lesson based on its Title and Full Transcript, then select between 1 and at most 3 most appropriate categories from the Allowed Categories list.

                Rules:
                1. Base your classification primarily on the actual content and discussion in the Full Transcript, not just superficial keywords in the Title.
                2. Select categories that represent the main topic or key themes of the lesson. Do NOT pick a category simply because a word was mentioned in passing.
                3. Choose between 1 and at most 3 categories. If only 1 category is truly relevant, return only 1 category. Do NOT try to force 3 categories.
                4. You MUST ONLY choose category codes that are explicitly listed in the Allowed Categories list.
                5. NEVER invent, hallucinate, or create new category codes.
            """;

    /** Lesson repository */
    private final LessonRepository lessonRepository;

    /** Transcription segment repository */
    private final TranscriptionSegmentRepository transcriptionSegmentRepository;

    /** Category repository */
    private final CategoryRepository categoryRepository;

    /** AI Core Client */
    private final AiCoreClient aiCoreClient;

    /** Cache manager */
    private final CacheManager cacheManager;

    /** Transaction template */
    private final TransactionTemplate transactionTemplate;

    /**
     * Analyzes lesson title and full transcript with AI, validates results against allowed categories, updates the
     * lesson-category relationships, and returns the categorized response.
     *
     * @param lessonId the lesson UUID
     * @return LessonCategoryAnalysisResponse with updated categories
     */
    public LessonCategoryAnalysisResponse classifyLessonCategories(UUID lessonId) {
        // 1. Load Lesson
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException(ErrorCode.LESSON_NOT_FOUND));

        // 2. Validate Title
        if (StringUtils.isBlank(lesson.getTitle())) {
            throw new ParroTalkException(ErrorCode.LESSON_TITLE_MISSING);
        }

        // 3. Load & Validate Full Transcript
        List<TranscriptionSegment> segments =
                transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId);
        if (segments == null || segments.isEmpty()) {
            throw new ParroTalkException(ErrorCode.LESSON_TRANSCRIPT_EMPTY);
        }

        String fullTranscript =
                segments.stream().filter(Objects::nonNull).map(segment -> segment.getText()).filter(Objects::nonNull)
                        .map(s -> s.trim()).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"));

        if (StringUtils.isBlank(fullTranscript)) {
            throw new ParroTalkException(ErrorCode.LESSON_TRANSCRIPT_EMPTY);
        }

        // 4. Load Allowed Categories
        List<Category> activeCategories = categoryRepository.findByStatus(CmsItemStatus.ACTIVE);
        if (activeCategories == null || activeCategories.isEmpty()) {
            throw new ParroTalkException(ErrorCode.ACTIVE_CATEGORIES_NOT_FOUND);
        }

        Map<String, Category> allowedCategoryMap = new LinkedHashMap<>();
        for (Category category : activeCategories) {
            String code = normalizeCategoryCode(category.getSlug());
            if (!code.isEmpty()) {
                allowedCategoryMap.put(code, category);
            }
        }

        if (allowedCategoryMap.isEmpty()) {
            throw new ParroTalkException(ErrorCode.ACTIVE_CATEGORIES_NOT_FOUND);
        }

        // 5. Build AI Request & Prompts
        String allowedCategoriesPrompt = buildAllowedCategoriesPrompt(allowedCategoryMap);
        String userPrompt = buildUserPrompt(lesson.getTitle().trim(), fullTranscript, allowedCategoriesPrompt);

        // 6. Call AI & 7. Validate AI Result
        List<Category> selectedCategories;
        try {
            selectedCategories = aiCoreClient.callAndValidate(
                    SYSTEM_PROMPT,
                    userPrompt,
                    LlmCategoryClassificationResponse.class,
                    (response) -> this.validateAiResponse(response, allowedCategoryMap));
        } catch (ParroTalkException e) {
            throw e;
        } catch (Exception e) {
            throw new ParroTalkException("Failed to analyze lesson categories with AI. Error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 8. Update Lesson Categories (Replace old categories with new ones)
        transactionTemplate.executeWithoutResult(status -> {
            lesson.setCategories(new HashSet<>(selectedCategories));
            lessonRepository.save(lesson);
        });

        // 9. Evict Cache
        evictLessonDetailCache(lessonId);

        // 10. Return Response
        List<LessonCategoryAnalysisResponse.CategoryItem> categoryItems = selectedCategories.stream()
                .map(
                        cat -> LessonCategoryAnalysisResponse.CategoryItem.builder().id(cat.getId())
                                .code(normalizeCategoryCode(cat.getSlug())).nameEn(cat.getName())
                                .nameVi(cat.getDescription() != null ? cat.getDescription() : cat.getName()).build())
                .collect(Collectors.toList());

        return LessonCategoryAnalysisResponse.builder().lessonId(lesson.getId()).title(lesson.getTitle())
                .categories(categoryItems).build();
    }

    /**
     * Build user prompt.
     * 
     * @param title lesson title
     * @param fullTranscript full transcript
     * @param allowedCategories list of allowed categories
     * @return user prompt
     */
    private String buildUserPrompt(String title, String fullTranscript, String allowedCategories) {
        return """
                Lesson Title:
                %s

                Full Transcript:
                %s

                Allowed Categories:
                %s
                """.formatted(title, fullTranscript, allowedCategories);
    }

    /**
     * Build allowed categories prompt.
     * 
     * @param categories list of categories
     * @return multi-line string contains list of allowed categories with format "CODE | Name EN | Name VN"
     */
    private String buildAllowedCategoriesPrompt(Map<String, Category> allowedCategoryMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Category> entry : allowedCategoryMap.entrySet()) {
            String code = entry.getKey();
            Category category = entry.getValue();
            String nameEn = category.getName() != null ? category.getName().trim() : "";
            String nameVi = category.getDescription() != null ? category.getDescription().trim() : nameEn;
            sb.append("- ").append(code).append(" | ").append(nameEn).append(" | ").append(nameVi).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Validate AI response and return list of categories.
     * 
     * @param aiResponse LLM category classification response
     * @param allowedCategoryMap map of allowed categories
     * @return list of categories
     */
    private List<Category> validateAiResponse(LlmCategoryClassificationResponse aiResponse,
            Map<String, Category> allowedCategoryMap) {
        if (aiResponse == null || aiResponse.categoryCodes() == null || aiResponse.categoryCodes().isEmpty()) {
            throw new ParroTalkException("AI failed to classify categories: no categories returned",
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }

        List<String> distinctCodes = aiResponse.categoryCodes()
                .stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (distinctCodes.size() > 3) {
            throw new ParroTalkException("AI returned " + distinctCodes.size() + " categories. Maximum allowed is 3.",
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }

        List<Category> result = new ArrayList<>();
        for (String code : distinctCodes) {
            Category category = allowedCategoryMap.get(code);
            if (category == null) {
                throw new ParroTalkException("AI returned unauthorized or unknown category code: " + code,
                        HttpStatus.UNPROCESSABLE_CONTENT);
            }
            result.add(category);
        }

        return result;
    }

    /**
     * Normalize category slug to uppercase with underscores.
     * 
     * @param slug category slug
     * @return normalized category code
     */
    public String normalizeCategoryCode(String slug) {
        if (slug == null) {
            return "";
        }
        return slug.trim().toUpperCase().replace('-', '_');
    }

    /**
     * Evict cache for lesson detail.
     * 
     * @param lessonId lesson id
     */
    private void evictLessonDetailCache(UUID lessonId) {
        try {
            Cache cache = cacheManager.getCache(CacheNames.LESSON_DETAIL_CACHE);
            if (cache != null) {
                cache.evict(lessonId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache for lessonId {}", lessonId, e);
        }
    }
}
