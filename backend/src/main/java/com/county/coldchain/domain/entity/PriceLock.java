package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.LockStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 批发客户锁价协议：锁定品类、单价与数量，调度需优先满足。 */
@Getter
@Setter
@Entity
@Table(name = "biz_price_lock")
public class PriceLock extends BaseEntity {
    private String lockNo;
    private Long customerId;
    private String categoryCode;
    private BigDecimal lockedPrice;
    private BigDecimal lockedQty;
    /** 已履约数量 */
    private BigDecimal fulfilledQty = BigDecimal.ZERO;
    private LocalDate validFrom;
    private LocalDate validTo;
    @Enumerated(EnumType.STRING)
    private LockStatus status = LockStatus.ACTIVE;
    private String remark;
}
