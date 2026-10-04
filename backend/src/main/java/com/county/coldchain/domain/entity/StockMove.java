package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.MoveReason;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存移动流水：上架、转仓、报损、退货出库，全程留痕。 */
@Getter
@Setter
@Entity
@Table(name = "biz_stock_move")
public class StockMove extends BaseEntity {
    private Long batchId;
    private Long lotId;
    private Long fromLocationId;
    private Long toLocationId;
    private Long fromZoneId;
    private Long toZoneId;
    @Enumerated(EnumType.STRING)
    private MoveReason reason;
    private BigDecimal weight;
    private String operator;
    private LocalDateTime movedAt;
    private String remark;
}
