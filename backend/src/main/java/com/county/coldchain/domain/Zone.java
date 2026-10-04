package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 冷库温区（保鲜温区）：冷藏/冷冻/冰温等。故障状态下在库批次需转仓，
 * 转仓事件改变库位与后续仓储费，并可能产生货损责任。
 */
@Entity
@Table(name = "zone")
@Getter
@Setter
@NoArgsConstructor
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false)
    private String name;

    /** 温区类型：冷藏 / 冷冻 / 冰温 */
    @Column(nullable = false, length = 20)
    private String zoneType;

    /** 目标温度区间（仅展示与巡检用） */
    @Column(precision = 5, scale = 2)
    private BigDecimal targetTemp;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal capacityKg;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal usedKg = BigDecimal.ZERO;

    /** 是否故障 */
    private boolean faulty = false;

    private LocalDateTime faultSince;
    private String faultNote;
}
