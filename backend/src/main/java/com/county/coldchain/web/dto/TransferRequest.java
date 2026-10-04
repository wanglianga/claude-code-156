package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest {
    @NotNull
    private Long lotId;
    /** 目标库位（决定目标温区） */
    @NotNull
    private Long toLocationId;
    private String operator;
    private String remark;
}
