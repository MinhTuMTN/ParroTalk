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

    /**
     * Find by user id and lesson id in list.
     *
     * @param userId User id
     * @param lessonIds Collection of lesson ids
     * @return List of user lesson progress
     */
    @org.springframework.data.jpa.repository.Query("SELECT ulp FROM UserLessonProgress ulp WHERE ulp.id.userId = :userId AND ulp.id.lessonId IN :lessonIds")
    java.util.List<UserLessonProgress> findByUserIdAndLessonIdIn(
            @org.springframework.data.repository.query.Param("userId") java.util.UUID userId,
            @org.springframework.data.repository.query.Param("lessonIds") java.util.Collection<java.util.UUID> lessonIds);
}
