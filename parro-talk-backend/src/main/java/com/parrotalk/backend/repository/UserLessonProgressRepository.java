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

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.LessonProgressRowProjection;
import com.parrotalk.backend.entity.UserLessonProgress;
import com.parrotalk.backend.entity.UserLessonProgress.UserLessonProgressId;

@Repository
public interface UserLessonProgressRepository
        extends JpaRepository<UserLessonProgress, UserLessonProgressId> {

    /**
     * Find progress summary and draft segment rows in a single LEFT JOIN projection query.
     *
     * @param userId   User id
     * @param lessonId Lesson id
     * @param mode     Dictation mode
     * @return List of projected progress and draft segment rows
     */
    @Query("""
            SELECT ulp.currentSegmentId AS currentSegmentId,
                   ulp.lastProgress AS lastProgress,
                   ulp.updatedAt AS updatedAt,
                   ds.id.segmentId AS segmentId,
                   ds.userAnswer AS userAnswer,
                   ds.score AS score,
                   ds.replayCount AS replayCount,
                   ds.hintCount AS hintCount,
                   ds.isCorrect AS correct,
                   ds.updatedAt AS draftUpdatedAt
            FROM UserLessonProgress ulp
            LEFT JOIN ulp.draftSegments ds
            WHERE ulp.id.userId = :userId
              AND ulp.id.lessonId = :lessonId
              AND ulp.id.dictationMode = :mode
            """)
    List<LessonProgressRowProjection> findProgressWithDrafts(
            @Param("userId") UUID userId,
            @Param("lessonId") UUID lessonId,
            @Param("mode") DictationMode mode);

    /**
     * Find lesson progress and the target draft segment in a single LEFT JOIN query without entity graphs.
     *
     * @param userId    User id
     * @param lessonId  Lesson id
     * @param segmentId Segment id
     * @param mode      Dictation mode
     * @return List containing at most one [UserLessonProgress, UserLessonDraftSegment] pair
     */
    @Query("""
            SELECT ulp, ds
            FROM UserLessonProgress ulp
            LEFT JOIN UserLessonDraftSegment ds
              ON ds.id.userId = ulp.id.userId
             AND ds.id.lessonId = ulp.id.lessonId
             AND ds.id.dictationMode = ulp.id.dictationMode
             AND ds.id.segmentId = :segmentId
            WHERE ulp.id.userId = :userId
              AND ulp.id.lessonId = :lessonId
              AND ulp.id.dictationMode = :mode
            """)
    List<Object[]> findProgressAndDraft(
            @Param("userId") UUID userId,
            @Param("lessonId") UUID lessonId,
            @Param("segmentId") UUID segmentId,
            @Param("mode") DictationMode mode);

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
