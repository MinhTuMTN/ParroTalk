package com.parrotalk.backend.repository.cms;

import com.parrotalk.backend.entity.cms.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
}
