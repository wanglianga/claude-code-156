package com.county.coldchain.repo;

import com.county.coldchain.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PriceLockRepo extends JpaRepository<PriceLock, Long> {
    List<PriceLock> findAllByOrderById();
    @Query("""
            select l from PriceLock l
            where l.customer.id = :customerId and l.category.id = :categoryId and l.grade = :grade
            and current_date between l.validFrom and l.validTo
            order by l.id desc""")
    List<PriceLock> findActive(Long customerId, Long categoryId, Domain.Grade grade);
}
