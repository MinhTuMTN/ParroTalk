package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.SegmentType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptionSegmentRepository extends JpaRepository<TranscriptionSegment, Long> {
    List<TranscriptionSegment> findByJobId(UUID jobId);
    List<TranscriptionSegment> findByJobIdAndTypeOrderByStartTimeAsc(UUID jobId, SegmentType type);
}
