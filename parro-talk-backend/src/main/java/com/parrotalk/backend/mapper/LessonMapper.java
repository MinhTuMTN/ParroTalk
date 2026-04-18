package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.LessonResponse;
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
    LessonResponse toLessonResponse(Lesson lesson);

}
