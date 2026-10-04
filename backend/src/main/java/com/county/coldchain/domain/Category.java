package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 品类：葡萄、草莓、叶菜、鲜肉、冻品、水产。
 * 携带温度阈值、各等级参考售价与默认费率，是计费与温控判定的主数据。
 */
@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** 到货温度上限（℃），超过即超标 */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxArrivalTemp;

    /** 建议保鲜温区名称（冷藏/冷冻/冰温） */
    @Column(nullable = false)
    private String targetZoneType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceA;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceB;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceC;

    /** 元/吨·天 仓储费 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal storageFeePerTonDay;

    /** 元/吨 分拣费 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sortFeePerTon;

    /** 建议货架期（天），用于管理端高风险批次判定 */
    @Column(nullable = false)
    private Integer shelfLifeDays;
}
