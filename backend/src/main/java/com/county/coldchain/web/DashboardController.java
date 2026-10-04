package com.county.coldchain.web;

import com.county.coldchain.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "经营总览（批次状态/在库重量/告警/催货/锁价）")
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return dashboardService.overview();
    }

    @Operation(summary = "在仓监控：停留天数/温区占用/可售重量变化/损耗责任/客户催货")
    @GetMapping("/storage-monitor")
    public List<Map<String, Object>> storageMonitor() {
        return dashboardService.storageMonitor();
    }

    @Operation(summary = "调度建议：高风险先出/锁价客户优先/转加工渠道")
    @GetMapping("/dispatch-suggestions")
    public List<Map<String, Object>> suggestions() {
        return dashboardService.dispatchSuggestions();
    }

    @Operation(summary = "温区占用与故障看板")
    @GetMapping("/zones")
    public List<Map<String, Object>> zones() {
        return dashboardService.zones();
    }
}
