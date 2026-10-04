package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ComplaintRequest {
    @NotNull
    private Long outboundId;
    /** BAD_FRUIT / SHORT_WEIGHT / TEMP_ABUSE / OTHER */
    @NotNull
    private String type;
    private BigDecimal problemWeight;
    private BigDecimal claimAmount;
    private String description;
}
