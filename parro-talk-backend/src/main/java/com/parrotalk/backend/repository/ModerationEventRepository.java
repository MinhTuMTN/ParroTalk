package com.parrotalk.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.ModerationTargetType;
import com.parrotalk.backend.entity.ModerationEvent;

@Repository
public interface ModerationEventRepository extends JpaRepository<ModerationEvent, UUID> {

    /**
     * Audit trail of one moderated item, oldest first.
     *
     * @param targetType Domain of the target
     * @param targetId   Target id
     * @return Ordered list of events
     */
    @EntityGraph(attributePaths = { "actor" })
    List<ModerationEvent> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
            ModerationTargetType targetType, UUID targetId);
}
