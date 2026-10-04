package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 出库单：支持优先出库、临时改客户、加工渠道；价格取锁价或品类参考价 */
@Entity
@Table(name = "outbound")
@Getter
@Setter
@NoArgsConstructor
public class Outbound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Domain.Grade grade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weight;

    /** 出库单价（锁价优先），元/kg */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Domain.Channel channel = Domain.Channel.NORMAL;

    /** 优先出库 */
    private boolean priority = false;

    /** 是否临时改客户 */
    private boolean customerChanged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private PriceLock priceLock;

    private LocalDateTime outboundAt;
    private String operator;
    private String remark;
}
