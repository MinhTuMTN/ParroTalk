package com.parrotalk.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.dto.LessonProgressRowProjection;
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
    @Mapping(target = "blankResults", ignore = true)
    @Mapping(target = "accuracy", ignore = true)
    DraftSegmentResponse toDraftSegmentResponse(UserLessonDraftSegment draft);

    /**
     * Convert LessonProgressRowProjection to DraftSegmentResponse.
     *
     * @param row LessonProgressRowProjection
     * @return DraftSegmentResponse
     */
    @Mapping(target = "score", source = "score", defaultValue = "0")
    @Mapping(target = "replayCount", source = "replayCount", defaultValue = "0")
    @Mapping(target = "hintCount", source = "hintCount", defaultValue = "0")
    @Mapping(target = "correct", source = "correct", defaultValue = "false")
    @Mapping(target = "updatedAt", source = "draftUpdatedAt")
    @Mapping(target = "blankResults", ignore = true)
    @Mapping(target = "accuracy", ignore = true)
    DraftSegmentResponse toDraftSegmentResponse(LessonProgressRowProjection row);
}
