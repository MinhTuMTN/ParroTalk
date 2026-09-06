package com.parrotalk.backend.mapper;

import java.util.List;
import java.util.Set;

import com.parrotalk.backend.dto.FeaturedLessonDTO;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
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
    LessonResponse toLessonResponse(Lesson lesson);

    /**
     * Convert lesson to featured lesson DTO.
     * 
     * @param lesson Lesson entity
     * @return Featured lesson DTO
     */
    FeaturedLessonDTO toFeaturedLessonDTO(Lesson lesson);

    /**
     * Map set of categories to list of category names.
     * 
     * @param categories Set of categories
     * @return List of category names
     */
    default List<String> mapCategories(Set<Category> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream().map(c -> c.getName()).toList();
    }
}
