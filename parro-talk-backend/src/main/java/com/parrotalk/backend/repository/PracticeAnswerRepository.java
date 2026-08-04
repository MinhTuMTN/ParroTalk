package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.PracticeAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, UUID> {
    List<PracticeAnswer> findBySessionIdOrderByAnsweredAtAsc(UUID sessionId);
    long countBySessionId(UUID sessionId);
}
