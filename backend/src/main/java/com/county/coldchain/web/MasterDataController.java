package com.county.coldchain.web;

import com.county.coldchain.domain.*;
import com.county.coldchain.repo.*;
import com.county.coldchain.service.BatchChainService;
import com.county.coldchain.service.QueryService;
import com.county.coldchain.web.dto.Dtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** 主数据：货主、客户、品类、温区、锁价、农户预付款 */
@RestController
@RequestMapping("/api")
public class MasterDataController {

    private final OwnerRepo ownerRepo;
    private final CustomerRepo customerRepo;
    private final CategoryRepo categoryRepo;
    private final ZoneRepo zoneRepo;
    private final PriceLockRepo priceLockRepo;
    private final QueryService queryService;

    public MasterDataController(OwnerRepo ownerRepo, CustomerRepo customerRepo,
                                CategoryRepo categoryRepo, ZoneRepo zoneRepo,
                                PriceLockRepo priceLockRepo, QueryService queryService) {
        this.ownerRepo = ownerRepo;
        this.customerRepo = customerRepo;
        this.categoryRepo = categoryRepo;
        this.zoneRepo = zoneRepo;
        this.priceLockRepo = priceLockRepo;
        this.queryService = queryService;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return queryService.masterData();
    }

    @PostMapping("/owners")
    public Owner createOwner(@RequestBody Dtos.OwnerReq req) {
        Owner o = new Owner();
        o.setCode(req.code());
        o.setName(req.name());
        o.setType(req.type());
        o.setPhone(req.phone());
        return ownerRepo.save(o);
    }

    /** 农户预付款入账（同一车货关联的预付款） */
    @PostMapping("/owners/{id}/advance")
    @Transactional
    public Owner advance(@PathVariable Long id, @RequestBody Dtos.AdvanceReq req) {
        Owner o = ownerRepo.findById(id).orElseThrow(() -> new ApiException("货主不存在"));
        if (req.amount() == null || req.amount().signum() == 0) {
            throw new ApiException("预付款金额必须非0（可负数冲减）");
        }
        o.setAdvanceBalance(o.getAdvanceBalance().add(req.amount()));
        return ownerRepo.save(o);
    }

    @PostMapping("/customers")
    public Customer createCustomer(@RequestBody Dtos.CustomerReq req) {
        Customer c = new Customer();
        c.setName(req.name());
        c.setPhone(req.phone());
        c.setContact(req.contact());
        return customerRepo.save(c);
    }

    @PostMapping("/categories")
    public Category createCategory(@RequestBody Dtos.CategoryReq req) {
        Category c = new Category();
        c.setName(req.name());
        c.setMaxArrivalTemp(req.maxArrivalTemp());
        c.setTargetZoneType(req.targetZoneType());
        c.setPriceA(req.priceA());
        c.setPriceB(req.priceB());
        c.setPriceC(req.priceC());
        c.setStorageFeePerTonDay(req.storageFeePerTonDay());
        c.setSortFeePerTon(req.sortFeePerTon());
        c.setShelfLifeDays(req.shelfLifeDays());
        return categoryRepo.save(c);
    }

    @PostMapping("/zones")
    public Zone createZone(@RequestBody Dtos.ZoneReq req) {
        Zone z = new Zone();
        z.setCode(req.code());
        z.setName(req.name());
        z.setZoneType(req.zoneType());
        z.setTargetTemp(req.targetTemp());
        z.setCapacityKg(req.capacityKg());
        return zoneRepo.save(z);
    }

    @PostMapping("/price-locks")
    public PriceLock createLock(@RequestBody Dtos.PriceLockReq req) {
        PriceLock l = new PriceLock();
        l.setCustomer(customerRepo.findById(req.customerId()).orElseThrow(() -> new ApiException("客户不存在")));
        l.setCategory(categoryRepo.findById(req.categoryId()).orElseThrow(() -> new ApiException("品类不存在")));
        l.setGrade(req.grade());
        l.setLockedPrice(req.lockedPrice());
        l.setValidFrom(LocalDate.parse(req.validFrom()));
        l.setValidTo(LocalDate.parse(req.validTo()));
        l.setLockedQtyKg(req.lockedQtyKg() == null ? BigDecimal.ZERO : req.lockedQtyKg());
        return priceLockRepo.save(l);
    }
}
