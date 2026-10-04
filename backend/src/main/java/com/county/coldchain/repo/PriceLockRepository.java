package com.county.coldchain.repo;

import com.county.coldchain.domain.DomainEnums.LockStatus;
import com.county.coldchain.domain.entity.PriceLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceLockRepository extends JpaRepository<PriceLock, Long> {
    List<PriceLock> findByStatus(LockStatus status);
    List<PriceLock> findByCustomerIdAndStatus(Long customerId, LockStatus status);
}
