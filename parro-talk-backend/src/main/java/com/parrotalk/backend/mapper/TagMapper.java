package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.TagResponse;
import com.parrotalk.backend.entity.Tag;
import org.mapstruct.Mapper;

/**
 * Tag mapper.
 * 
 * @author MinhTuMTN
 */
@Mapper(componentModel = "spring")
public interface TagMapper {
    /**
     * Convert tag to tag response.
     * 
     * @param tag Tag entity
     * @return Tag response
     */
    TagResponse toTagResponse(Tag tag);
}
