package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.WritingGeneratedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WritingGeneratedItemRepository extends JpaRepository<WritingGeneratedItem, UUID> {

    List<WritingGeneratedItem> findBySessionIdOrderByDisplayOrderAsc(UUID sessionId);
}

