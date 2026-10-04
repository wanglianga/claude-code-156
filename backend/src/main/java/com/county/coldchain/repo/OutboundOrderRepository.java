package com.county.coldchain.repo;

import com.county.coldchain.domain.DomainEnums.OrderStatus;
import com.county.coldchain.domain.entity.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
    List<OutboundOrder> findByBatchIdOrderByCreatedAtDesc(Long batchId);
    List<OutboundOrder> findByStatus(OrderStatus status);
    List<OutboundOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
