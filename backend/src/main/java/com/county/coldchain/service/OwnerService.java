package com.county.coldchain.service;

import com.county.coldchain.domain.entity.AdvanceLedger;
import com.county.coldchain.domain.entity.Owner;
import com.county.coldchain.repo.AdvanceLedgerRepository;
import com.county.coldchain.repo.OwnerRepository;
import com.county.coldchain.support.BizException;
import com.county.coldchain.web.dto.AdvanceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 货主预付款/赊欠台账：同一车货放款、账单结算回收都在这里联动。 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final AdvanceLedgerRepository advanceRepository;

    @Transactional
    public Owner save(Owner owner) {
        if (owner.getAdvanceBalance() == null) {
            owner.setAdvanceBalance(java.math.BigDecimal.ZERO);
        }
        return ownerRepository.save(owner);
    }

    @Transactional(readOnly = true)
    public List<Owner> list() {
        return ownerRepository.findAll();
    }

    /** 放款（冷库预付给货主，direction=ADVANCE，余额增加表示冷库应收）。 */
    @Transactional
    public AdvanceLedger advance(Long ownerId, AdvanceRequest req) {
        Owner o = ownerRepository.findById(ownerId).orElseThrow(() -> new BizException("货主不存在"));
        if (req.getAmount() == null || req.getAmount().signum() <= 0) {
            throw new BizException("预付款金额必须大于 0");
        }
        AdvanceLedger l = new AdvanceLedger();
        l.setOwnerId(o.getId());
        l.setBatchId(req.getBatchId());
        l.setDirection("ADVANCE");
        l.setAmount(req.getAmount());
        l.setRemark(req.getRemark());
        l.setOccurredAt(LocalDateTime.now());
        advanceRepository.save(l);
        o.setAdvanceBalance(o.getAdvanceBalance().add(req.getAmount()));
        ownerRepository.save(o);
        return l;
    }

    @Transactional(readOnly = true)
    public List<AdvanceLedger> ledger(Long ownerId) {
        return advanceRepository.findByOwnerIdOrderByOccurredAtAsc(ownerId);
    }
}
