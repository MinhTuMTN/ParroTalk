package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserLessonAnswerDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLessonAnswerDetailRepository extends JpaRepository<UserLessonAnswerDetail, UUID> {
    List<UserLessonAnswerDetail> findByHistory_HistoryId(UUID historyId);
}
