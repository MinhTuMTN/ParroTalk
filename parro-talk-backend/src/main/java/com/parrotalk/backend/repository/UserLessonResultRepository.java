package com.parrotalk.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.entity.UserLessonResult;

@Repository
public interface UserLessonResultRepository extends JpaRepository<UserLessonResult, UUID> {
    Optional<UserLessonResult> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    Optional<UserLessonResult> findByUserIdAndLessonIdAndDictationMode(
            UUID userId,
            UUID lessonId,
            DictationMode dictationMode);
}
