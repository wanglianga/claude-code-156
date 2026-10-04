package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 账单明细行：按等级列示货值，以及各项费用/扣款 */
@Entity
@Table(name = "bill_line")
@Getter
@Setter
@NoArgsConstructor
public class BillLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Bill bill;

    /** GOODS / STORAGE / SORT / LOSS / RETURN / COMPLAINT / ADVANCE */
    @Column(nullable = false, length = 16)
    private String lineType;

    @Column(nullable = false, length = 120)
    private String summary;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weight = BigDecimal.ZERO;

    /** 单价 元/kg（费用行为空） */
    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** 金额：货值为正，费用/扣款为负向列示（amount 带符号） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
}
