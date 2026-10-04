package com.county.coldchain.web;

import com.county.coldchain.domain.DomainEnums;
import com.county.coldchain.domain.entity.Batch;
import com.county.coldchain.service.BatchService;
import com.county.coldchain.web.dto.ArrivalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "到货登记")
@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "到货登记（批次链起点，自动风控识别）")
    @PostMapping("/arrival")
    public Batch arrival(@Valid @RequestBody ArrivalRequest req) {
        return batchService.register(req);
    }

    @Operation(summary = "批次列表（可按状态过滤，逗号分隔）")
    @GetMapping
    public List<Batch> list(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return batchService.list(null);
        }
        return batchService.list(Arrays.stream(status.split(","))
                .map(String::trim).map(DomainEnums.BatchStatus::valueOf).toList());
    }

    @Operation(summary = "批次详情")
    @GetMapping("/{id}")
    public Batch get(@PathVariable Long id) {
        return batchService.get(id);
    }

    @Operation(summary = "同车批次（混批混装核查）")
    @GetMapping("/by-vehicle/{vehicleNo}")
    public List<Batch> sameVehicle(@PathVariable String vehicleNo) {
        return batchService.sameVehicle(vehicleNo);
    }
}
