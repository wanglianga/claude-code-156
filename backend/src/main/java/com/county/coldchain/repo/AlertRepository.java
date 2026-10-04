package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByResolvedFalseOrderByCreatedAtDesc();
    List<Alert> findByBatchIdOrderByCreatedAtDesc(Long batchId);
    List<Alert> findByZoneIdAndResolvedFalse(Long zoneId);
}
