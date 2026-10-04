package com.county.coldchain.service;

import com.county.coldchain.domain.entity.PriceLock;
import com.county.coldchain.repo.PriceLockRepository;
import com.county.coldchain.support.BizException;
import com.county.coldchain.web.dto.PriceLockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 批发客户锁价协议维护。 */
@Service
@RequiredArgsConstructor
public class PriceLockService {

    private final PriceLockRepository repository;

    @Transactional
    public PriceLock create(PriceLockRequest req) {
        if (req.getLockedPrice() == null || req.getLockedQty() == null) {
            throw new BizException("锁价单价与锁定数量必填");
        }
        PriceLock l = new PriceLock();
        l.setLockNo("K" + System.currentTimeMillis() % 1000000000L);
        l.setCustomerId(req.getCustomerId());
        l.setCategoryCode(req.getCategoryCode());
        l.setLockedPrice(req.getLockedPrice());
        l.setLockedQty(req.getLockedQty());
        l.setValidFrom(req.getValidFrom());
        l.setValidTo(req.getValidTo());
        l.setRemark(req.getRemark());
        return repository.save(l);
    }

    @Transactional
    public PriceLock cancel(Long id) {
        PriceLock l = repository.findById(id).orElseThrow(() -> new BizException("锁价单不存在"));
        l.setStatus(com.county.coldchain.domain.DomainEnums.LockStatus.CANCELLED);
        return repository.save(l);
    }

    @Transactional(readOnly = true)
    public List<PriceLock> list(String status) {
        if (status == null || status.isBlank()) {
            return repository.findAll();
        }
        return repository.findByStatus(com.county.coldchain.domain.DomainEnums.LockStatus.valueOf(status));
    }
}
