package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ComplaintResolveRequest {
    /** OWNER / WAREHOUSE / CARRIER / CUSTOMER */
    @NotNull
    private String responsibility;
    @NotNull
    private BigDecimal settledAmount;
    private String handler;
    private String handleResult;
}
