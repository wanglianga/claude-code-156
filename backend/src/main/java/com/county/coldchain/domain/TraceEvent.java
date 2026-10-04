package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 批次链事件流水：入库、质检、分拣、库位、出库、损耗、投诉、改客户、催货、
 * 温区故障、转仓、退货、账单、结算全部落到同一条链上，供监管追溯与责任认定。
 */
@Entity
@Table(name = "trace_event", indexes = {
        @Index(name = "idx_trace_batch", columnList = "batch_id"),
        @Index(name = "idx_trace_seq", columnList = "seq")
})
@Getter
@Setter
@NoArgsConstructor
public class TraceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    /** 链内顺序号（同批次自增） */
    @Column(nullable = false)
    private Integer seq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 18)
    private Domain.EventType type;

    @Column(nullable = false, length = 2000)
    private String detail;

    private String operator;
    private LocalDateTime occurredAt;

    public TraceEvent(Batch batch, Integer seq, Domain.EventType type, String detail, String operator, LocalDateTime occurredAt) {
        this.batch = batch;
        this.seq = seq;
        this.type = type;
        this.detail = detail;
        this.operator = operator;
        this.occurredAt = occurredAt;
    }
}
