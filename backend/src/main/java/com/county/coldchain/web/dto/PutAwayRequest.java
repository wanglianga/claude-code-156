package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 选定库位上架（分拣/直接入库后）。lotId+weight 上架到 locationId。 */
@Data
public class PutAwayRequest {
    @NotNull
    private Long lotId;
    @NotNull
    private Long locationId;
    private String operator;
    private String remark;
}
