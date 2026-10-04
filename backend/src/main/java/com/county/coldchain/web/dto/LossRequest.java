package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LossRequest {
    @NotNull
    private Long lotId;
    @NotNull
    private BigDecimal weight;
    /** WAREHOUSE 冷库责任 / OWNER 货主责任，决定账单由谁承担 */
    @NotNull
    private String responsibility;
    private String operator;
    private String remark;
}
