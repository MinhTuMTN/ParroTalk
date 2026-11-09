package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Idiom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdiomRepository extends JpaRepository<Idiom, UUID> {
}

