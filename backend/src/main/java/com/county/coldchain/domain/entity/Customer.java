package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 批发/加工客户（预计出库客户）。 */
@Getter
@Setter
@Entity
@Table(name = "biz_customer")
public class Customer extends BaseEntity {
    private String name;
    private String contact;
    private String phone;
    /** 是否加工渠道客户（降级货可转入） */
    private Boolean processingChannel = false;
}
