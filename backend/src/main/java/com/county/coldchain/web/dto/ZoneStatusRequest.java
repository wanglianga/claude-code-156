package com.county.coldchain.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ZoneStatusRequest {
    private Boolean fault;
    private String faultNote;
    private BigDecimal currentTemp;
}
