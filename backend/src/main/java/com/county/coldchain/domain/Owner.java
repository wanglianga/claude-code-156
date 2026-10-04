package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 货主：农户 / 合作社 / 批发商。advanceBalance 为已支付的预付款余额（结算时抵扣）。 */
@Entity
@Table(name = "owner")
@Getter
@Setter
@NoArgsConstructor
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Domain.OwnerType type;

    private String phone;

    /** 平台已向货主支付的预付款（结算时从应付款中抵扣） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal advanceBalance = BigDecimal.ZERO;
}
