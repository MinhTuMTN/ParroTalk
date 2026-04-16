package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserLessonDraftSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLessonDraftSegmentRepository
        extends JpaRepository<UserLessonDraftSegment, UserLessonDraftSegment.UserLessonDraftSegmentId> {

    List<UserLessonDraftSegment> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    void deleteByUserIdAndLessonId(UUID userId, UUID lessonId);
}
