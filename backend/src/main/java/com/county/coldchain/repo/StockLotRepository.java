package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.StockLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockLotRepository extends JpaRepository<StockLot, Long> {
    List<StockLot> findByBatchId(Long batchId);
    List<StockLot> findByStatus(String status);
    List<StockLot> findByZoneIdAndStatus(Long zoneId, String status);
}
