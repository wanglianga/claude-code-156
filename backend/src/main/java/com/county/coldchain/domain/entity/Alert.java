package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.AlertLevel;
import com.county.coldchain.domain.DomainEnums.AlertType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 风险告警：温度超标/混批混装/临时改客户/优先出库/温区故障/
 * 分拣损耗争议/按原重结算争议/客户投诉/在仓超时/锁价待履约。
 * 所有告警都挂批次（温区故障可空），形成调度看板。
 */
@Getter
@Setter
@Entity
@Table(name = "biz_alert")
public class Alert extends BaseEntity {
    @Enumerated(EnumType.STRING)
    private AlertType type;
    @Enumerated(EnumType.STRING)
    private AlertLevel level;
    private Long batchId;
    private Long zoneId;
    private String title;
    private String detail;
    private Boolean resolved = false;
    private String resolution;
    private String raisedBy;
}
