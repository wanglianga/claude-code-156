package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货主预付款/赊欠流水：同一车货可能已拿预付款。
 * direction=ADVANCE 冷库预付给货主（应收），REPAY 货主还款/账单抵扣。
 */
@Getter
@Setter
@Entity
@Table(name = "biz_advance_ledger")
public class AdvanceLedger extends BaseEntity {
    private Long ownerId;
    private Long batchId;
    /** ADVANCE 放款（增加应收） / REPAY 回收（账单抵扣或现金还款） */
    private String direction;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime occurredAt;
}
