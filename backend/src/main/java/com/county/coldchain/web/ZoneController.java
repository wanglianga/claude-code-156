package com.county.coldchain.web;

import com.county.coldchain.domain.Zone;
import com.county.coldchain.service.BatchChainService;
import com.county.coldchain.web.dto.Dtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 温区故障上报/恢复：冷库温区故障会写入在库批次链并触发转仓流程 */
@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final BatchChainService chain;

    public ZoneController(BatchChainService chain) {
        this.chain = chain;
    }

    @PutMapping("/{id}/fault")
    public Zone fault(@PathVariable Long id, @RequestBody Dtos.ZoneFaultReq req) {
        return chain.setZoneFault(id, req);
    }
}
