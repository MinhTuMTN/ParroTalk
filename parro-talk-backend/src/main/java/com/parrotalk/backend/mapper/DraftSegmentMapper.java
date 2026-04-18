package com.parrotalk.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.entity.UserLessonDraftSegment;

/**
 * Draft segment mapper.
 */
@Mapper(componentModel = "spring")
public interface DraftSegmentMapper {

    /**
     * Convert UserLessonDraftSegment to DraftSegmentResponse.
     * 
     * @param draft UserLessonDraftSegment
     * @return DraftSegmentResponse
     */
    @Mapping(target = "segmentId", source = "id.segmentId")
    DraftSegmentResponse toDraftSegmentResponse(UserLessonDraftSegment draft);
}
