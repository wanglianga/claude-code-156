package com.county.coldchain.repo;

import com.county.coldchain.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DemandRepo extends JpaRepository<CustomerDemand, Long> {
    List<CustomerDemand> findByStatusOrderByCreatedAtDesc(Domain.DemandStatus status);
    List<CustomerDemand> findByBatchIdOrderByCreatedAtDesc(Long batchId);
}
