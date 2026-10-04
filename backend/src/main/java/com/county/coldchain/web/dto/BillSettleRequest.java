package com.county.coldchain.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillSettleRequest {
    /** 实际支付（现金/转账），为空则按应付净额减预付款抵扣自动计算 */
    private BigDecimal paidAmount;
    private String remark;
}
