package com.county.coldchain.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PriceLockRequest {
    private Long customerId;
    private String categoryCode;
    private BigDecimal lockedPrice;
    private BigDecimal lockedQty;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String remark;
}
