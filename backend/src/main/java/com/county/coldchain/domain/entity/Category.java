package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.ZoneType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 品类档案：定义到货温度阈值、建议温区、默认仓储费/分拣费、默认售价。 */
@Getter
@Setter
@Entity
@Table(name = "biz_category")
public class Category extends BaseEntity {
    /** GRAPE 葡萄 / STRAWBERRY 草莓 / LEAFY 叶菜 / FRESH_MEAT 鲜肉 / FROZEN 冻品 / SEAFOOD 水产 */
    private String code;
    private String name;
    /** 到货温度上限（℃），超出即告警 */
    private BigDecimal maxArrivalTemp;
    /** 到货温度下限（℃），如冻品要求 ≤ -15 */
    private BigDecimal minArrivalTemp;
    private ZoneType recommendedZone;
    /** 元/公斤·天 */
    private BigDecimal storageFeeRate;
    /** 元/公斤（分拣服务费） */
    private BigDecimal sortFeeRate;
    /** 元/公斤（损耗扣款阈值内的约定损耗率） */
    private BigDecimal agreedLossRate;
    private BigDecimal defaultPriceA;
    private BigDecimal defaultPriceB;
    private BigDecimal defaultPriceProcess;
}
