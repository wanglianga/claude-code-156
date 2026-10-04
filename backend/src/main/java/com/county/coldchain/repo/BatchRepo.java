package com.county.coldchain.repo;

import com.county.coldchain.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BatchRepo extends JpaRepository<Batch, Long> {
    long countByStatus(Domain.BatchStatus status);
    List<Batch> findAllByOrderByArrivalAtDesc();
    @Query("select coalesce(max(seq),0) from TraceEvent e where e.batch.id = :batchId")
    int maxSeq(Long batchId);
}
