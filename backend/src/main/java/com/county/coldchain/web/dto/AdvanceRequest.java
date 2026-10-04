package com.county.coldchain.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdvanceRequest {
    private Long batchId;
    private BigDecimal amount;
    private String remark;
}
