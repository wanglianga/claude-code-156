package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.Grade;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 分拣结果明细：每个等级的重量与售价，直接决定库位温区与货主结算。 */
@Getter
@Setter
@Entity
@Table(name = "biz_sorting_item")
public class SortItem extends BaseEntity {
    private Long sortingId;
    @Enumerated(EnumType.STRING)
    private Grade grade;
    /** 该等级重量（公斤） */
    private BigDecimal weight;
    /** 结算售价（元/公斤） */
    private BigDecimal unitPrice;
    /** 建议保鲜天数 */
    private Integer shelfLifeDays;
    /** 该档建议温区（由品类带出，可改） */
    private String zoneCode;
}
