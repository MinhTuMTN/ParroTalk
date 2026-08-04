package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.PracticeSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, UUID> {
    Page<PracticeSession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);
}
