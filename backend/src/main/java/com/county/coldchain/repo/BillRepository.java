package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByBatchIdOrderByCreatedAtDesc(Long batchId);
    List<Bill> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<Bill> findFirstByBatchIdAndStatus(Long batchId, com.county.coldchain.domain.DomainEnums.BillStatus status);
}
