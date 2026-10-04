package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ArrivalRequest {
    @NotNull
    private Long ownerId;
    @NotNull
    private Long categoryId;
    private String origin;
    private LocalDate productionDate;
    @NotNull
    private BigDecimal grossWeight;
    private BigDecimal tareWeight;
    @NotNull
    private BigDecimal arrivalTemp;
    private String packaging;
    private String certificateNo;
    private String issuingOrg;
    private String certIssueDate;
    private Boolean certificateVerified;
    private String vehicleNo;
    private Boolean mixedLoad;
    private Long plannedCustomerId;
    @NotNull
    private String settleMethod;
    private BigDecimal lockedPrice;
    private Boolean settleByOriginalWeight;
    /** 操作人 */
    private String operator;
}
