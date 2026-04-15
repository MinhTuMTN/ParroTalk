package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(source = "description", target = "content")
    @Mapping(target = "categories", ignore = true)
    LessonResponse toLessonResponse(Lesson lesson);

}
