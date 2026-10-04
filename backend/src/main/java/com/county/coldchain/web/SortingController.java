package com.county.coldchain.web;

import com.county.coldchain.domain.entity.SortItem;
import com.county.coldchain.domain.entity.SortingRecord;
import com.county.coldchain.service.SortingService;
import com.county.coldchain.web.dto.SortingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "分拣处理")
@RestController
@RequestMapping("/api/sortings")
@RequiredArgsConstructor
public class SortingController {

    private final SortingService sortingService;

    @Data
    public static class DisputeResolveRequest {
        private Boolean acceptLoss;
        private BigDecimal adjudicatedLoss;
        private String note;
    }

    @Operation(summary = "提交分拣结果（等级/重量/定价/工时/损耗争议）")
    @PostMapping
    public SortingRecord sort(@Valid @RequestBody SortingRequest req) {
        return sortingService.sort(req);
    }

    @Operation(summary = "批次分拣记录")
    @GetMapping("/batch/{batchId}")
    public List<SortingRecord> byBatch(@PathVariable Long batchId) {
        return sortingService.listByBatch(batchId);
    }

    @Operation(summary = "分拣明细")
    @GetMapping("/{id}/items")
    public List<SortItem> items(@PathVariable Long id) {
        return sortingService.items(id);
    }

    @Operation(summary = "裁定分拣损耗争议")
    @PostMapping("/{id}/resolve-dispute")
    public SortingRecord resolve(@PathVariable Long id, @RequestBody DisputeResolveRequest req) {
        return sortingService.resolveDispute(id, Boolean.TRUE.equals(req.getAcceptLoss()),
                req.getAdjudicatedLoss(), req.getNote());
    }
}
