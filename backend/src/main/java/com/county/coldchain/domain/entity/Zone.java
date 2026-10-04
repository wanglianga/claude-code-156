package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.ZoneType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 冷库温区，含故障状态（温区故障告警会阻塞该温区出库/上架并推动转仓）。 */
@Getter
@Setter
@Entity
@Table(name = "biz_zone")
public class Zone extends BaseEntity {
    private String code;
    private String name;
    @Enumerated(EnumType.STRING)
    private ZoneType type;
    /** 目标温度（℃） */
    private BigDecimal targetTemp;
    /** 容量（公斤） */
    private BigDecimal capacityKg;
    /** 当前占用（公斤） */
    private BigDecimal occupiedKg = BigDecimal.ZERO;
    /** 实时温度（℃），由巡检/传感器更新 */
    private BigDecimal currentTemp;
    private Boolean fault = false;
    private String faultNote;
}
