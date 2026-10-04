package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.SortingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SortingRecordRepository extends JpaRepository<SortingRecord, Long> {
    List<SortingRecord> findByBatchIdOrderBySortedAtAsc(Long batchId);
}
