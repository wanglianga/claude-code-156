package com.county.coldchain.repo;

import com.county.coldchain.domain.DomainEnums.BatchStatus;
import com.county.coldchain.domain.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    Optional<Batch> findByBatchNo(String batchNo);

    List<Batch> findByVehicleNo(String vehicleNo);

    List<Batch> findByStatusIn(List<BatchStatus> statuses);

    @Query("select coalesce(max(b.id),0) from Batch b")
    long maxId();
}
