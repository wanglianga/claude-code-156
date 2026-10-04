package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.OutboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboundItemRepository extends JpaRepository<OutboundItem, Long> {
    List<OutboundItem> findByOutboundId(Long outboundId);
    List<OutboundItem> findByLotId(Long lotId);
}
