package com.parrotalk.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.UserLessonProgress;

@Repository
public interface UserLessonProgressRepository
        extends JpaRepository<UserLessonProgress, UserLessonProgress.UserLessonProgressId> {

    /**
     * Find by user id and lesson id.
     * 
     * @param userId   User id
     * @param lessonId Lesson id
     * @return User lesson progress
     */
    @EntityGraph(attributePaths = { "draftSegments" })
    Optional<UserLessonProgress> findById(UserLessonProgress.UserLessonProgressId id);
}
