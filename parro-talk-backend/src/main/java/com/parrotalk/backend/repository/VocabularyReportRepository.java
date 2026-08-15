package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.VocabularyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for VocabularyReport.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface VocabularyReportRepository extends JpaRepository<VocabularyReport, UUID> {
}
