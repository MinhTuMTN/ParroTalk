package com.parrotalk.backend.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parrotalk.backend.entity.AiUsageEvent;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, UUID> {

    @Modifying
    @Query("DELETE FROM AiUsageEvent e WHERE e.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
