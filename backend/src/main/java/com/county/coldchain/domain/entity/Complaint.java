package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.ComplaintType;
import com.county.coldchain.domain.DomainEnums.Responsibility;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 出库后客户投诉（坏果/短重/温度），可回挂出库单与批次，触发追溯与赔付扣款。 */
@Getter
@Setter
@Entity
@Table(name = "biz_complaint")
public class Complaint extends BaseEntity {
    private String complaintNo;
    private Long outboundId;
    private Long batchId;
    private Long customerId;
    @Enumerated(EnumType.STRING)
    private ComplaintType type;
    /** 投诉坏果/问题重量（公斤） */
    private BigDecimal problemWeight;
    /** 诉求赔付金额（元） */
    private BigDecimal claimAmount;
    /** 裁定赔付金额（元） */
    private BigDecimal settledAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private Responsibility responsibility = Responsibility.PENDING;
    private String description;
    private String handler;
    private LocalDateTime handledAt;
    private String handleResult;
    private Boolean resolved = false;
}
