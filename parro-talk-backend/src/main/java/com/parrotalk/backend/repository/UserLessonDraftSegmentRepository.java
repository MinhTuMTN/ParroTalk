package com.parrotalk.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.entity.UserLessonDraftSegment;

/**
 * User lesson draft segment repository.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface UserLessonDraftSegmentRepository
        extends JpaRepository<UserLessonDraftSegment, UserLessonDraftSegment.UserLessonDraftSegmentId> {

    /**
     * Find exact draft segment
     * 
     * @param userId    User id
     * @param lessonId  Lesson id
     * @param segmentId Segment id
     * @return Optional UserLessonDraftSegment
     */
    @Query("""
            select u from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.segmentId = :segmentId
              and u.id.dictationMode = com.parrotalk.backend.constant.DictationMode.FULL
            """)
    Optional<UserLessonDraftSegment> findExact(
            UUID userId,
            UUID lessonId,
            UUID segmentId);

    /**
     * Find exact draft segment for a specific dictation mode.
     */
    @Query("""
            select u from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.segmentId = :segmentId
              and u.id.dictationMode = :mode
            """)
    Optional<UserLessonDraftSegment> findExactByMode(
            UUID userId,
            UUID lessonId,
            UUID segmentId,
            DictationMode mode);

    /**
     * Find by user id and lesson id
     * 
     * @param userId   User id
     * @param lessonId Lesson id
     * @return List UserLessonDraftSegment
     */
    @Query("""
            select u from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.dictationMode = com.parrotalk.backend.constant.DictationMode.FULL
            """)
    List<UserLessonDraftSegment> findByUserIdAndLessonId(
            UUID userId,
            UUID lessonId);

    /**
     * Find by user id, lesson id, and dictation mode.
     */
    @Query("""
            select u from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.dictationMode = :mode
            """)
    List<UserLessonDraftSegment> findByUserIdAndLessonIdAndMode(
            UUID userId,
            UUID lessonId,
            DictationMode mode);

    /**
     * Count completed (correct) draft segments for a user, lesson, and dictation mode.
     */
    @Query("""
            select count(u) from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.dictationMode = :mode
              and u.isCorrect = true
            """)
    long countCorrectByUserIdAndLessonIdAndMode(
            UUID userId,
            UUID lessonId,
            DictationMode mode);

    /**
     * Delete by user id and lesson id
     * 
     * @param userId   User id
     * @param lessonId Lesson id
     */
    @Modifying
    @Query("""
            delete from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.dictationMode = com.parrotalk.backend.constant.DictationMode.FULL
            """)
    void deleteByUserIdAndLessonId(
            UUID userId,
            UUID lessonId);

    /**
     * Delete by user id, lesson id, and dictation mode.
     */
    @Modifying
    @Query("""
            delete from UserLessonDraftSegment u
            where u.id.userId = :userId
              and u.id.lessonId = :lessonId
              and u.id.dictationMode = :mode
            """)
    void deleteByUserIdAndLessonIdAndMode(
            UUID userId,
            UUID lessonId,
            DictationMode mode);
}
