package com.parrotalk.backend.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.parrotalk.backend.dto.FeaturedLessonDTO;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import org.hibernate.Hibernate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Lesson mapper.
 * 
 * @author MinhTuMTN
 */
@Mapper(componentModel = "spring")
public interface LessonMapper {

    /**
     * Convert lesson to lesson response.
     * 
     * @param lesson Lesson entity
     * @return Lesson response
     */
    @Mapping(target = "segments", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "translationSummary", ignore = true)
    @Mapping(target = "thumbnailUrl", expression = "java(resolveThumbnailUrl(lesson))")
    LessonResponse toLessonResponse(Lesson lesson);

    /**
     * Convert lesson to featured lesson DTO.
     * 
     * @param lesson Lesson entity
     * @return Featured lesson DTO
     */
    @Mapping(target = "thumbnailUrl", expression = "java(resolveThumbnailUrl(lesson))")
    FeaturedLessonDTO toFeaturedLessonDTO(Lesson lesson);

    /**
     * Resolve lesson thumbnail URL, falling back to its category thumbnail when missing or default.
     *
     * @param lesson Lesson entity
     * @return Resolved thumbnail URL
     */
    default String resolveThumbnailUrl(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        String thumb = lesson.getThumbnailUrl();
        boolean hasCustomThumb = thumb != null && !thumb.isBlank() && !thumb.contains("default-audio-thumbnail");
        if (hasCustomThumb) {
            return thumb;
        }
        if (lesson.getCategories() != null && Hibernate.isInitialized(lesson.getCategories())) {
            String categoryImage = lesson.getCategories().stream()
                    .filter(c -> c != null && c.getImageUrl() != null && !c.getImageUrl().isBlank())
                    .sorted(Comparator.comparing(
                            (Category c) -> c.getSortOrder() != null ? c.getSortOrder() : Integer.MAX_VALUE)
                            .thenComparing(c -> c.getName() != null ? c.getName() : ""))
                    .map(Category::getImageUrl)
                    .findFirst()
                    .orElse(null);
            if (categoryImage != null) {
                return categoryImage;
            }
        }
        return thumb;
    }

    /**
     * Map set of categories to list of category names.
     * 
     * @param categories Set of categories
     * @return List of category names
     */
    default List<String> mapCategories(Set<Category> categories) {
        if (categories == null || !Hibernate.isInitialized(categories)) {
            return List.of();
        }
        return categories.stream().map(c -> c.getName()).toList();
    }
}
