package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.TraceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceEventRepository extends JpaRepository<TraceEvent, Long> {
    List<TraceEvent> findByBatchIdOrderByEventAtAsc(Long batchId);
}
