package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 温区内库位。 */
@Getter
@Setter
@Entity
@Table(name = "biz_location")
public class Location extends BaseEntity {
    private String code;
    private Long zoneId;
    private BigDecimal capacityKg;
    private BigDecimal occupiedKg = BigDecimal.ZERO;
}
