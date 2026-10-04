package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 出库明细：按库位批次（等级）出库，价格可取锁价或分拣定价。 */
@Getter
@Setter
@Entity
@Table(name = "biz_outbound_item")
public class OutboundItem extends BaseEntity {
    private Long outboundId;
    private Long lotId;
    private String grade;
    private BigDecimal weight;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
