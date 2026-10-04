package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库位库存（按“批次 × 等级”落位）：分拣结果影响冷库库位与保鲜温区。
 * 转仓时在同批次同等级下关闭旧 lot、新建 lot，保证批次链库位历史可追。
 */
@Entity
@Table(name = "storage_lot")
@Getter
@Setter
@NoArgsConstructor
public class StorageLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Zone zone;

    @Column(nullable = false, length = 40)
    private String locationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Domain.Grade grade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weight;

    /** 起算仓储费日期（转仓的新 lot 以转仓日起算） */
    @Column(nullable = false)
    private LocalDate storageFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Domain.LotStatus status = Domain.LotStatus.IN_ZONE;

    private LocalDate closedAt;

    /** 转仓来源 lot */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private StorageLot sourceLot;
}
