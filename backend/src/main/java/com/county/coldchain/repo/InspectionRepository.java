package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByBatchIdOrderByInspectedAtDesc(Long batchId);
    Optional<Inspection> findFirstByBatchIdOrderByInspectedAtDesc(Long batchId);
}
