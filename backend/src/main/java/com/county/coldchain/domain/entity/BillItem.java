package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.BillItemType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 账单明细：收入为正、费用扣款为负（amount 带符号，贷方/借方由 type 决定说明）。 */
@Getter
@Setter
@Entity
@Table(name = "biz_bill_item")
public class BillItem extends BaseEntity {
    private Long billId;
    @Enumerated(EnumType.STRING)
    private BillItemType type;
    private String summary;
    /** 重量（公斤，可空） */
    private BigDecimal weight;
    /** 单价/费率 */
    private BigDecimal unitPrice;
    /** 金额：收入正、扣款负 */
    private BigDecimal amount;
    /** 天数（仓储费） */
    private Integer days;
}
