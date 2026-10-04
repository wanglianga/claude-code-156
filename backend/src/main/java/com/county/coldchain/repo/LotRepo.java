package com.county.coldchain.repo;

import com.county.coldchain.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LotRepo extends JpaRepository<StorageLot, Long> {
    List<StorageLot> findByBatchIdOrderByIdAsc(Long batchId);
    List<StorageLot> findByStatusOrderByBatchArrivalAtAsc(Domain.LotStatus status);
}
