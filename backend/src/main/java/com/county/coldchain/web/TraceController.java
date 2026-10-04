package com.county.coldchain.web;

import com.county.coldchain.service.TraceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "监管追溯")
@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceQueryService traceQueryService;

    @Operation(summary = "批次链全景：入库→质检→分拣→库位→出库→损耗→结算→最终客户")
    @GetMapping("/batch/{batchId}")
    public Map<String, Object> chain(@PathVariable Long batchId) {
        return traceQueryService.chain(batchId);
    }
}
