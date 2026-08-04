package com.parrotalk.backend.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.AppFeedback;

@Repository
public interface AppFeedbackRepository
        extends JpaRepository<AppFeedback, UUID>, JpaSpecificationExecutor<AppFeedback> {

    /**
     * Admin listing with associations pre-fetched.
     *
     * @param specification Filter built by {@code AppFeedbackSpecification}
     * @param pageable      Paging and sorting
     * @return Page of feedback
     */
    @Override
    @EntityGraph(attributePaths = { "user", "moderationState.assignee" })
    Page<AppFeedback> findAll(Specification<AppFeedback> specification, Pageable pageable);

    /**
     * Feedback submitted by a user.
     *
     * @param userId   User id
     * @param pageable Paging and sorting
     * @return Page of feedback
     */
    @EntityGraph(attributePaths = { "user", "moderationState.assignee" })
    Page<AppFeedback> findByUserId(UUID userId, Pageable pageable);
}
