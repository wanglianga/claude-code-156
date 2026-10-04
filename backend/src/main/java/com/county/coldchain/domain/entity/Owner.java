package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.OwnerType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 货主：农户 / 合作社 / 批发商。预付款余额体现同一车货关联的农户预付款。 */
@Getter
@Setter
@Entity
@Table(name = "biz_owner")
public class Owner extends BaseEntity {
    private String name;
    private String contact;
    private String phone;
    @Enumerated(EnumType.STRING)
    private OwnerType type;
    private String village;
    /** 预付/赊欠账户余额（可为负：货主欠款；正：冷库预付未结） */
    private BigDecimal advanceBalance = BigDecimal.ZERO;
}
