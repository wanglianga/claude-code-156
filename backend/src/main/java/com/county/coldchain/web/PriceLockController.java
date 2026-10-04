package com.county.coldchain.web;

import com.county.coldchain.domain.entity.PriceLock;
import com.county.coldchain.service.PriceLockService;
import com.county.coldchain.web.dto.PriceLockRequest;
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

@Tag(name = "客户锁价")
@RestController
@RequestMapping("/api/price-locks")
@RequiredArgsConstructor
public class PriceLockController {

    private final PriceLockService service;

    @Operation(summary = "新建锁价协议")
    @PostMapping
    public PriceLock create(@RequestBody PriceLockRequest req) {
        return service.create(req);
    }

    @Operation(summary = "取消锁价")
    @PostMapping("/{id}/cancel")
    public PriceLock cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @Operation(summary = "锁价列表（ACTIVE/FULFILLED/CANCELLED）")
    @GetMapping
    public List<PriceLock> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }
}
