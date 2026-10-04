package com.county.coldchain.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OutboundRequest {
    @NotNull
    private Long customerId;
    @NotNull
    private Long batchId;
    private Boolean priority;
    /** 手动指定单价（按原重结算等特殊场景），为空则取货位价格/锁价 */
    private BigDecimal overrideUnitPrice;
    private String operator;
    private String remark;
    @NotNull
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long lotId;
        @NotNull
        private BigDecimal weight;
        /** 可空：不传则取锁价/货位价 */
        private BigDecimal unitPrice;
    }
}
