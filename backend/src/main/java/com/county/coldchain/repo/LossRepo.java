package com.county.coldchain.repo;

import com.county.coldchain.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LossRepo extends JpaRepository<LossRecord, Long> {
    List<LossRecord> findByBatchIdOrderByLossAtAsc(Long batchId);
}
