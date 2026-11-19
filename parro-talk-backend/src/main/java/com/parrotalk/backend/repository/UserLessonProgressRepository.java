package com.parrotalk.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.UserLessonProgress;
import com.parrotalk.backend.entity.UserLessonProgress.UserLessonProgressId;

@Repository
public interface UserLessonProgressRepository
        extends JpaRepository<UserLessonProgress, UserLessonProgressId> {

    /**
     * Find by user id, lesson id, and mode.
     *
     * @param id Composite progress id
     * @return User lesson progress
     */
    @EntityGraph(attributePaths = { "draftSegments" })
    Optional<UserLessonProgress> findById(UserLessonProgressId id);

    /**
     * Find by user id and lesson id in list.
     *
     * @param userId    User id
     * @param lessonIds Collection of lesson ids
     * @return List of user lesson progress
     */
    @Query("SELECT ulp FROM UserLessonProgress ulp WHERE ulp.id.userId = :userId AND ulp.id.lessonId IN :lessonIds")
    List<UserLessonProgress> findByUserIdAndLessonIdIn(
            @Param("userId") UUID userId,
            @Param("lessonIds") Collection<UUID> lessonIds);
}
