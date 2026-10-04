package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分拣记录：一批货按等级拆成分拣结果，记录投入工时（分拣工时也是经营成本）。
 * 分拣结果直接决定可售重量、等级、损耗率与货主结算。
 */
@Entity
@Table(name = "sort_record")
@Getter
@Setter
@NoArgsConstructor
public class SortRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    private String operator;
    private LocalDateTime sortedAt;

    /** 投入分拣工时（工时数，可小数） */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal laborHours = BigDecimal.ZERO;

    /** 工时单价 元/工时（分拣费可按重量或按工时计） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal laborRate = BigDecimal.ZERO;

    /** 投线重量 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal inputWeight;

    /** A/B/C 各等级产出重量 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weightA = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weightB = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weightC = BigDecimal.ZERO;

    /** 分拣损耗重量 = 投线 - 各级产出 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lossWeight = BigDecimal.ZERO;

    /** 损耗责任方：分拣损耗争议的落点 */
    @Enumerated(EnumType.STRING)
    @Column(length = 14)
    private Domain.Responsible lossResponsible = Domain.Responsible.OWNER;

    private boolean disputed = false;
    private String disputeNote;

    private String remark;
}
