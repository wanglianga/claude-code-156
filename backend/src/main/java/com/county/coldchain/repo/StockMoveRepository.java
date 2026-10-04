package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.StockMove;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMoveRepository extends JpaRepository<StockMove, Long> {
    List<StockMove> findByBatchIdOrderByMovedAtAsc(Long batchId);
    List<StockMove> findByLotIdOrderByMovedAtAsc(Long lotId);
}
