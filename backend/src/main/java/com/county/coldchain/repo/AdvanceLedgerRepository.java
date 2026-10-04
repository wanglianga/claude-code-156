package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.AdvanceLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvanceLedgerRepository extends JpaRepository<AdvanceLedger, Long> {
    List<AdvanceLedger> findByOwnerIdOrderByOccurredAtAsc(Long ownerId);
    List<AdvanceLedger> findByBatchId(Long batchId);
}
