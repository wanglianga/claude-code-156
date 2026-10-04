package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.Grade;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库内库存批次（货位批次）：分拣/入库后按等级生成，占据库位与温区。
 * 重量变化（出库、报损、转仓拆分）实时回写，是"可售重量变化"的最小跟踪单元。
 */
@Getter
@Setter
@Entity
@Table(name = "biz_stock_lot")
public class StockLot extends BaseEntity {
    private String lotNo;
    private Long batchId;
    @Enumerated(EnumType.STRING)
    private Grade grade;
    private Long zoneId;
    private Long locationId;
    /** 入库重量（公斤） */
    private BigDecimal initialWeight;
    /** 当前重量（公斤） */
    private BigDecimal currentWeight;
    /** 累计报损重量 */
    private BigDecimal lossWeight = BigDecimal.ZERO;
    /** 累计出库重量 */
    private BigDecimal outWeight = BigDecimal.ZERO;
    /** 结算售价（元/公斤，来自分拣定价/降级定价） */
    private BigDecimal unitPrice;
    /** 建议保鲜天数 */
    private Integer shelfLifeDays;
    private LocalDateTime inAt;
    /** IN 在库 / OUT 清库 / LOST 报损清空 */
    private String status = "IN";
}
