package com.county.coldchain.web;

import com.county.coldchain.domain.entity.Alert;
import com.county.coldchain.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "风险告警")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Data
    public static class ResolveRequest {
        private String resolution;
    }

    @Operation(summary = "未处理告警列表")
    @GetMapping("/open")
    public List<Alert> open() {
        return alertService.openAlerts();
    }

    @Operation(summary = "批次告警")
    @GetMapping("/batch/{batchId}")
    public List<Alert> batch(@PathVariable Long batchId) {
        return alertService.batchAlerts(batchId);
    }

    @Operation(summary = "处理/关闭告警")
    @PostMapping("/{id}/resolve")
    public Alert resolve(@PathVariable Long id, @RequestBody ResolveRequest req) {
        return alertService.resolve(id, req == null ? "" : req.getResolution());
    }
}
