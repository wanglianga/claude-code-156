package com.county.coldchain.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 客户催货：管理端调度看板用于决定优先出库顺序 */
@Entity
@Table(name = "customer_demand")
@Getter
@Setter
@NoArgsConstructor
public class CustomerDemand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    /** 客户要求优先出库 */
    private boolean priority = false;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Domain.DemandStatus status = Domain.DemandStatus.OPEN;

    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;
}
