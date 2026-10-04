package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 质检记录：外观/成熟度/腐损/含水/温度/票证六维验收 */
@Entity
@Table(name = "qc_record")
@Getter
@Setter
@NoArgsConstructor
public class QcRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    private String inspector;
    private LocalDateTime checkedAt;

    /** 1-5 分（5 最好） */
    private Integer appearanceScore;
    private Integer maturityScore;
    /** 腐损率 % 与含水率 %（0-100，可一位小数） */
    @Column(precision = 5, scale = 2)
    private BigDecimal rotRate;
    @Column(precision = 5, scale = 2)
    private BigDecimal moistureRate;
    @Column(precision = 5, scale = 2)
    private BigDecimal measuredTemp;

    /** 票证是否齐全（检疫证明） */
    private boolean documentsOk;

    /** 温度是否超标（系统据阈值给出） */
    private boolean tempExceeded;

    /** 验收后定级 */
    @Enumerated(EnumType.STRING)
    @Column(length = 4)
    private Domain.Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Domain.QcResult result;

    /** 验收接受入仓重量（退货场景为 0 或部分） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal acceptedWeight;

    /** 退货重量 */
    @Column(precision = 12, scale = 2)
    private BigDecimal returnWeight = BigDecimal.ZERO;

    /** 降级差价说明、争议备注 */
    private String remark;
}
