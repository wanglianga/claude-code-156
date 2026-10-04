package com.county.coldchain.web;

import com.county.coldchain.domain.entity.Inspection;
import com.county.coldchain.service.InspectionService;
import com.county.coldchain.web.dto.InspectionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "质检验收")
@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @Operation(summary = "提交质检结论：ACCEPT 入库 / SORT 分拣 / DOWNGRADE 降级 / RETURN 退货")
    @PostMapping
    public Inspection inspect(@Valid @RequestBody InspectionRequest req) {
        return inspectionService.inspect(req);
    }

    @Operation(summary = "批次质检记录")
    @GetMapping("/batch/{batchId}")
    public List<Inspection> byBatch(@PathVariable Long batchId) {
        return inspectionService.listByBatch(batchId);
    }
}
