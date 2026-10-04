package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 报损记录：在仓损耗/坏果报损，责任方决定扣款承担方式，直接减少可售重量 */
@Entity
@Table(name = "loss_record")
@Getter
@Setter
@NoArgsConstructor
public class LossRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private StorageLot lot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Domain.Grade grade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weight;

    /** 报损原因（坏果/温区故障/超期/温度超标） */
    @Column(nullable = false, length = 100)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private Domain.Responsible responsible = Domain.Responsible.OWNER;

    private LocalDateTime lossAt;
    private String operator;
}
