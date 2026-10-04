package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.SortItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SortItemRepository extends JpaRepository<SortItem, Long> {
    List<SortItem> findBySortingId(Long sortingId);
}
