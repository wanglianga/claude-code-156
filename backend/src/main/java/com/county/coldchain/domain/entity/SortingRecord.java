package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分拣记录：一次分拣作业的主单。
 * 分拣工时（低温仓位作业）通过 laborMinutes 进入账单。
 */
@Getter
@Setter
@Entity
@Table(name = "biz_sorting")
public class SortingRecord extends BaseEntity {
    private String sortingNo;
    private Long batchId;
    private String sorter;
    private LocalDateTime sortedAt;
    /** 分拣前投入重量（公斤） */
    private BigDecimal inputWeight;
    /** 分拣后各等级可售合计（公斤，由明细汇总） */
    private BigDecimal outputWeight = BigDecimal.ZERO;
    /** 分拣损耗重量（公斤，= input - output） */
    private BigDecimal lossWeight = BigDecimal.ZERO;
    /** 分拣损耗率（0-1） */
    private BigDecimal lossRate = BigDecimal.ZERO;
    /** 分拣工时（分钟） */
    private Integer laborMinutes = 0;
    /** 货主对损耗有争议 */
    private Boolean disputed = false;
    /** 争议说明 / 裁定备注 */
    private String disputeNote;
    /** 争议是否已裁定（裁定后报损重量才会计入结算） */
    private Boolean disputeResolved = false;
    /** 裁定最终损耗重量（冷库责任差额 = lossWeight - adjudicatedLossWeight） */
    private BigDecimal adjudicatedLossWeight;
    private String remark;
}
