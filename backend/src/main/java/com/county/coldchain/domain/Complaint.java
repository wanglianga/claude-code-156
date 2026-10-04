package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 出库后客户投诉坏果：按坏果重量与单价折算赔付，冲减货主结算款 */
@Entity
@Table(name = "complaint")
@Getter
@Setter
@NoArgsConstructor
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Outbound outbound;

    @Column(nullable = false, length = 200)
    private String content;

    /** 坏果重量 kg */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal badWeight;

    /** 赔付单价 元/kg（默认取出库单价） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** 赔付金额 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal compensation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private Domain.Responsible responsible = Domain.Responsible.OWNER;

    private LocalDateTime complainedAt;
    private String handler;
    private boolean resolved = true;
}
