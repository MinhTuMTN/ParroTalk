package com.parrotalk.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.UserLessonProgress;

@Repository
public interface UserLessonProgressRepository
        extends JpaRepository<UserLessonProgress, UserLessonProgress.UserLessonProgressId> {
}
