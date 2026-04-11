package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    Optional<Lesson> findByFileHash(String fileHash);
    List<Lesson> findAllByOwnerId(UUID ownerId);

}
