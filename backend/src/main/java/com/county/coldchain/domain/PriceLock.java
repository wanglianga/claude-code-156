package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 批发客户锁价：对某品类某等级锁定出库价；已锁价客户在调度中优先满足 */
@Entity
@Table(name = "price_lock")
@Getter
@Setter
@NoArgsConstructor
public class PriceLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Domain.Grade grade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lockedPrice;

    private LocalDate validFrom;
    private LocalDate validTo;

    /** 锁货量 kg（可选，0 表示不限量） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lockedQtyKg = BigDecimal.ZERO;
}
