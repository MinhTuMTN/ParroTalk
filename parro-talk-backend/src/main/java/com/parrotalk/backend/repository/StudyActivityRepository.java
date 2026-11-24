package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {

    Optional<StudyActivity> findByUserIdAndActivityDate(UUID userId, LocalDate activityDate);

    List<StudyActivity> findAllByUserIdOrderByActivityDateAsc(UUID userId);

    /**
     * Atomically insert or accumulate daily study activity in PostgreSQL and return the updated segments_completed.
     *
     * @param userId                 User id
     * @param activityDate           Activity date
     * @param studySecondsDelta      Study seconds delta
     * @param completedSegmentsDelta Completed segments delta
     * @param completedLessonsDelta  Completed lessons delta
     * @param now                    Current timestamp
     * @return Updated segments_completed count
     */
    @Query(value = """
            INSERT INTO study_activity (
                user_id,
                activity_date,
                study_seconds,
                segments_completed,
                lessons_completed,
                first_activity_at,
                last_activity_at,
                created_at,
                updated_at,
                is_deleted
            ) VALUES (
                :userId,
                :activityDate,
                :studySecondsDelta,
                :completedSegmentsDelta,
                :completedLessonsDelta,
                :now,
                :now,
                :now,
                :now,
                false
            )
            ON CONFLICT (user_id, activity_date) DO UPDATE SET
                study_seconds = study_activity.study_seconds + EXCLUDED.study_seconds,
                segments_completed = study_activity.segments_completed + EXCLUDED.segments_completed,
                lessons_completed = study_activity.lessons_completed + EXCLUDED.lessons_completed,
                last_activity_at = EXCLUDED.last_activity_at,
                updated_at = EXCLUDED.updated_at,
                is_deleted = false
            RETURNING segments_completed
            """, nativeQuery = true)
    int upsertAndReturnSegmentsCompleted(
            @Param("userId") UUID userId,
            @Param("activityDate") LocalDate activityDate,
            @Param("studySecondsDelta") int studySecondsDelta,
            @Param("completedSegmentsDelta") int completedSegmentsDelta,
            @Param("completedLessonsDelta") int completedLessonsDelta,
            @Param("now") LocalDateTime now);
}
