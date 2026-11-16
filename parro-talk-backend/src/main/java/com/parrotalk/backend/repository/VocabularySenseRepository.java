package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.VocabularySense;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularySenseRepository extends JpaRepository<VocabularySense, UUID> {
}

