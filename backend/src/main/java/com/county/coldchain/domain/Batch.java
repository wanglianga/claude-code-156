package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 到货批次：批次链的根。同一车货可能关联预付款、锁价、分拣工时、低温仓位，
 * 任何降级/转仓/报损都会通过事件链影响最终账单。
 */
@Entity
@Table(name = "batch")
@Getter
@Setter
@NoArgsConstructor
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 批次号，如 P20261004-001 */
    @Column(nullable = false, unique = true, length = 40)
    private String batchNo;

    @Column(nullable = false)
    private String vehicleNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    /** 产地 */
    @Column(nullable = false)
    private String origin;

    /** 到货重量（kg）——“原重” */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal arrivalWeight;

    /** 到货温度 ℃ */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal arrivalTemp;

    /** 包装形式（泡沫箱/筐/散货…） */
    @Column(nullable = false, length = 60)
    private String packaging;

    /** 检疫证明编号 */
    @Column(nullable = false, length = 60)
    private String quarantineNo;

    /** 预计出库客户（可被临时改客户事件覆盖） */
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer plannedCustomer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Domain.SettleMethod settleMethod = Domain.SettleMethod.ACTUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Domain.BatchStatus status = Domain.BatchStatus.REGISTERED;

    /** 混批混装标记 */
    private boolean mixedLoad = false;

    private LocalDateTime arrivalAt;
    private LocalDate inboundDate;

    /** 质检后实际入仓可售重量（kg），随分拣/报损/出库持续变化 */
    @Column(precision = 12, scale = 2)
    private BigDecimal saleableWeight = BigDecimal.ZERO;

    /** 已出库重量 */
    @Column(precision = 12, scale = 2)
    private BigDecimal shippedWeight = BigDecimal.ZERO;

    /** 累计报损重量 */
    @Column(precision = 12, scale = 2)
    private BigDecimal lossWeight = BigDecimal.ZERO;

    /** 累计退货重量 */
    @Column(precision = 12, scale = 2)
    private BigDecimal returnedWeight = BigDecimal.ZERO;

    /** 投诉扣重（坏果赔付，kg，按售价折算） */
    @Column(precision = 12, scale = 2)
    private BigDecimal complaintDeductWeight = BigDecimal.ZERO;

    /** 投诉扣款合计（元） */
    @Column(precision = 14, scale = 2)
    private BigDecimal complaintDeductAmount = BigDecimal.ZERO;

    /** 监管追溯终点备注（最终客户流向） */
    private String finalDestination;
}
