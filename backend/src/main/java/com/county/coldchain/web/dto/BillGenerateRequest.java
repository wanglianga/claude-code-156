package com.county.coldchain.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillGenerateRequest {
    private Long batchId;
    /** 抵扣预付款金额（不超过货主账户应收余额），为空默认抵扣全部余额 */
    private BigDecimal advanceDeduct;
    private String remark;
}
