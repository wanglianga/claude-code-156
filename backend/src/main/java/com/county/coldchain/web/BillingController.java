package com.county.coldchain.web;

import com.county.coldchain.domain.entity.Bill;
import com.county.coldchain.domain.entity.BillItem;
import com.county.coldchain.service.BillingService;
import com.county.coldchain.web.dto.BillGenerateRequest;
import com.county.coldchain.web.dto.BillSettleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "财务结算")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @Operation(summary = "按批次生成账单（可售重量/等级/仓储费/分拣费/工时/损耗/退货/投诉/预付款）")
    @PostMapping("/generate")
    public Bill generate(@RequestBody BillGenerateRequest req) {
        return billingService.generate(req);
    }

    @Operation(summary = "确认账单")
    @PostMapping("/{id}/confirm")
    public Bill confirm(@PathVariable Long id) {
        return billingService.confirm(id);
    }

    @Operation(summary = "结算付款（预付款自动抵扣落账）")
    @PostMapping("/{id}/settle")
    public Bill settle(@PathVariable Long id, @RequestBody(required = false) BillSettleRequest req) {
        return billingService.settle(id, req == null ? new BillSettleRequest() : req);
    }

    @Operation(summary = "账单列表（可按货主）")
    @GetMapping
    public List<Bill> list(@RequestParam(required = false) Long ownerId) {
        return billingService.list(ownerId);
    }

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    public Bill detail(@PathVariable Long id) {
        return billingService.detail(id);
    }

    @Operation(summary = "账单明细")
    @GetMapping("/{id}/items")
    public List<BillItem> items(@PathVariable Long id) {
        return billingService.items(id);
    }
}
