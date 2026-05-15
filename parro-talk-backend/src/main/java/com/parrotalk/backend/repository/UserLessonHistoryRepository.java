package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserLessonHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLessonHistoryRepository extends JpaRepository<UserLessonHistory, UUID> {

    List<UserLessonHistory> findByUserId(UUID userId);
}
