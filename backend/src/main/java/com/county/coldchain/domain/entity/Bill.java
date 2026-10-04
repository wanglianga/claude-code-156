package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.BillStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 货主批次账单：按实际可售重量、等级、仓储费、分拣费、损耗与退货生成。 */
@Getter
@Setter
@Entity
@Table(name = "biz_bill")
public class Bill extends BaseEntity {
    private String billNo;
    private Long batchId;
    private Long ownerId;
    /** 结算截止时间点（仓储费算到此时） */
    private LocalDateTime cutoffAt;
    /** 应付货主合计（销售收入 - 各项费用/扣款） */
    private BigDecimal netPayable = BigDecimal.ZERO;
    /** 销售/可售货值合计 */
    private BigDecimal grossValue = BigDecimal.ZERO;
    /** 费用扣款合计 */
    private BigDecimal totalDeduction = BigDecimal.ZERO;
    /** 账单中抵扣的预付款 */
    private BigDecimal advanceDeducted = BigDecimal.ZERO;
    /** 本次实付（现金/转账） */
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private BillStatus status = BillStatus.DRAFT;
    private String remark;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;
}
