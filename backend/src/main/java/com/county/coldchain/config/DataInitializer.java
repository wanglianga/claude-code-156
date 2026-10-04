package com.county.coldchain.config;

import com.county.coldchain.domain.*;
import com.county.coldchain.repo.*;
import com.county.coldchain.security.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 演示主数据与账号种子 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepo userRepo;
    private final CategoryRepo categoryRepo;
    private final OwnerRepo ownerRepo;
    private final CustomerRepo customerRepo;
    private final ZoneRepo zoneRepo;
    private final PriceLockRepo priceLockRepo;
    private final AuthService authService;

    public DataInitializer(UserRepo userRepo, CategoryRepo categoryRepo,
                           OwnerRepo ownerRepo, CustomerRepo customerRepo,
                           ZoneRepo zoneRepo, PriceLockRepo priceLockRepo,
                           AuthService authService) {
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepo;
        this.ownerRepo = ownerRepo;
        this.customerRepo = customerRepo;
        this.zoneRepo = zoneRepo;
        this.priceLockRepo = priceLockRepo;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            userRepo.save(new User("admin", authService.hash("admin123"), "系统管理员", "ADMIN"));
            userRepo.save(new User("qc", authService.hash("qc123"), "质检员王敏", "QC"));
            userRepo.save(new User("warehouse", authService.hash("wh123"), "库管老赵", "WAREHOUSE"));
            userRepo.save(new User("finance", authService.hash("fin123"), "财务小李", "FINANCE"));
        }
        if (categoryRepo.count() == 0) {
            cat("葡萄", "8", "冷藏", "12.00", "9.00", "6.00", "3.50", "60.00", 10);
            cat("草莓", "6", "冰温", "30.00", "22.00", "14.00", "5.00", "90.00", 5);
            cat("叶菜", "10", "冷藏", "7.00", "5.00", "3.00", "3.00", "50.00", 4);
            cat("鲜肉", "4", "冷藏", "38.00", "32.00", "26.00", "4.00", "70.00", 6);
            cat("冻品", "-12", "冷冻", "25.00", "20.00", "15.00", "6.00", "40.00", 180);
            cat("水产", "-2", "冰温", "45.00", "36.00", "26.00", "6.50", "100.00", 3);
        }
        if (ownerRepo.count() == 0) {
            owner("N001", "阳光种植合作社", Domain.OwnerType.COOP, "13800000001", "20000.00");
            owner("N002", "果农张建国", Domain.OwnerType.FARMER, "13800000002", "5000.00");
            owner("N003", "顺发水产批发商", Domain.OwnerType.WHOLESALER, "13800000003", "0.00");
        }
        if (customerRepo.count() == 0) {
            customer("城东鲜果连锁", "13900000001", "陈店长");
            customer("惠民超市采购部", "13900000002", "刘采购");
            customer("鲜味加工厂", "13900000003", "周厂长");
        }
        if (zoneRepo.count() == 0) {
            zone("CL1", "1号冷藏库", "冷藏", "2", "10000");
            zone("CL2", "2号冷藏库", "冷藏", "4", "8000");
            zone("IT1", "冰温库", "冰温", "-1", "5000");
            zone("FZ1", "冷冻库", "冷冻", "-18", "8000");
        }
        if (priceLockRepo.count() == 0) {
            lock(2L, 1L, Domain.Grade.A, "11.50", "3000");
            lock(1L, 2L, Domain.Grade.A, "28.00", "1000");
        }
    }

    private void cat(String name, String maxTemp, String zone, String pA, String pB, String pC,
                     String feeDay, String sortFee, int shelf) {
        Category c = new Category();
        c.setName(name);
        c.setMaxArrivalTemp(new BigDecimal(maxTemp));
        c.setTargetZoneType(zone);
        c.setPriceA(new BigDecimal(pA));
        c.setPriceB(new BigDecimal(pB));
        c.setPriceC(new BigDecimal(pC));
        c.setStorageFeePerTonDay(new BigDecimal(feeDay));
        c.setSortFeePerTon(new BigDecimal(sortFee));
        c.setShelfLifeDays(shelf);
        categoryRepo.save(c);
    }

    private void owner(String code, String name, Domain.OwnerType type, String phone, String advance) {
        Owner o = new Owner();
        o.setCode(code);
        o.setName(name);
        o.setType(type);
        o.setPhone(phone);
        o.setAdvanceBalance(new BigDecimal(advance));
        ownerRepo.save(o);
    }

    private void customer(String name, String phone, String contact) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setContact(contact);
        customerRepo.save(c);
    }

    private void zone(String code, String name, String type, String temp, String cap) {
        Zone z = new Zone();
        z.setCode(code);
        z.setName(name);
        z.setZoneType(type);
        z.setTargetTemp(new BigDecimal(temp));
        z.setCapacityKg(new BigDecimal(cap));
        zoneRepo.save(z);
    }

    private void lock(Long customerId, Long categoryId, Domain.Grade grade, String price, String qty) {
        PriceLock l = new PriceLock();
        l.setCustomer(customerRepo.findById(customerId).orElseThrow());
        l.setCategory(categoryRepo.findById(categoryId).orElseThrow());
        l.setGrade(grade);
        l.setLockedPrice(new BigDecimal(price));
        l.setValidFrom(LocalDate.now().minusDays(1));
        l.setValidTo(LocalDate.now().plusDays(30));
        l.setLockedQtyKg(new BigDecimal(qty));
        priceLockRepo.save(l);
    }
}
