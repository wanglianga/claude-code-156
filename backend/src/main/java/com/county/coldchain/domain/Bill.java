package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 货主结算账单：按实际可售重量（或货主要求按原重）×等级售价形成货值，
 * 扣仓储费、分拣费、冷库责任外的损耗、退货货值、投诉赔付，再抵扣农户预付款。
 * 任何一次降级、转仓、报损都会在重新生成账单时体现。
 */
@Entity
@Table(name = "bill")
@Getter
@Setter
@NoArgsConstructor
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String billNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Owner owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Domain.SettleMethod settleMethod;

    /** 结算计价重量（ACTUAL=可售，ORIGINAL=到货原重） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal settleWeight;

    /** 货值合计 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal goodsAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal storageFee = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sortFee = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lossDeduction = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal returnDeduction = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal complaintDeduction = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    /** 应付款 = 货值 - 各项费用/扣款 - 预付款抵扣 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Domain.BillStatus status = Domain.BillStatus.DRAFT;

    private LocalDate settleDate;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillLine> lines = new ArrayList<>();
}
