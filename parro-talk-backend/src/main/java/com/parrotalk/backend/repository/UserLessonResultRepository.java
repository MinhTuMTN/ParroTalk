package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserLessonResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLessonResultRepository extends JpaRepository<UserLessonResult, UUID> {
    Optional<UserLessonResult> findByUserIdAndLessonId(UUID userId, UUID lessonId);
}
