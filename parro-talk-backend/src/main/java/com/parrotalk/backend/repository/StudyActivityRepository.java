package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {

    Optional<StudyActivity> findByUserIdAndActivityDate(UUID userId, LocalDate activityDate);

    List<StudyActivity> findAllByUserIdOrderByActivityDateAsc(UUID userId);
}
