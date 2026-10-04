package com.county.coldchain.web;

import com.county.coldchain.domain.entity.AdvanceLedger;
import com.county.coldchain.domain.entity.Owner;
import com.county.coldchain.service.OwnerService;
import com.county.coldchain.web.dto.AdvanceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "货主预付款")
@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @Operation(summary = "货主预付款放款（同一车货关联农户预付款）")
    @PostMapping("/{id}/advance")
    public AdvanceLedger advance(@PathVariable Long id, @RequestBody AdvanceRequest req) {
        return ownerService.advance(id, req);
    }

    @Operation(summary = "预付款台账流水")
    @GetMapping("/{id}/advances")
    public List<AdvanceLedger> ledger(@PathVariable Long id) {
        return ownerService.ledger(id);
    }
}
