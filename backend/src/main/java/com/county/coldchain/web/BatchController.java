package com.county.coldchain.web;

import com.county.coldchain.domain.*;
import com.county.coldchain.repo.*;
import com.county.coldchain.service.BatchChainService;
import com.county.coldchain.service.QueryService;
import com.county.coldchain.web.dto.Dtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 批次链全部业务动作入口 */
@RestController
@RequestMapping("/api")
public class BatchController {

    private final BatchChainService chain;
    private final QueryService query;
    private final BatchRepo batchRepo;
    private final EventRepo eventRepo;
    private final BillRepo billRepo;

    public BatchController(BatchChainService chain, QueryService query,
                           BatchRepo batchRepo, EventRepo eventRepo, BillRepo billRepo) {
        this.chain = chain;
        this.query = query;
        this.batchRepo = batchRepo;
        this.eventRepo = eventRepo;
        this.billRepo = billRepo;
    }

    private static String op(HttpServletRequest req) {
        User u = (User) req.getAttribute(AuthInterceptor.ATTR_USER);
        return u.getName() + "(" + u.getRole() + ")";
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return query.dashboard();
    }

    @GetMapping("/batches")
    public List<Batch> batches() {
        return batchRepo.findAllByOrderByArrivalAtDesc();
    }

    @GetMapping("/batches/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return query.batchDetail(id);
    }

    @GetMapping("/batches/{id}/trace")
    public List<TraceEvent> trace(@PathVariable Long id) {
        return eventRepo.findByBatchIdOrderBySeqAsc(id);
    }

    @PostMapping("/batches/register")
    public Batch register(@RequestBody Dtos.RegisterReq req, HttpServletRequest http) {
        return chain.register(req, op(http));
    }

    @PostMapping("/qc")
    public QcRecord qc(@RequestBody Dtos.QcReq req, HttpServletRequest http) {
        return chain.qc(req, op(http));
    }

    @PostMapping("/sort")
    public SortRecord sort(@RequestBody Dtos.SortReq req, HttpServletRequest http) {
        return chain.sort(req, op(http));
    }

    @PostMapping("/loss")
    public BatchChainService.LossReportView loss(@RequestBody Dtos.LossReq req, HttpServletRequest http) {
        return chain.reportLoss(req, op(http));
    }

    @PostMapping("/outbound")
    public Outbound outbound(@RequestBody Dtos.OutboundReq req, HttpServletRequest http) {
        return chain.outbound(req, op(http));
    }

    @PostMapping("/transfer")
    public List<StorageLot> transfer(@RequestBody Dtos.TransferReq req, HttpServletRequest http) {
        return chain.transfer(req, op(http));
    }

    @PostMapping("/customer-change")
    public Batch customerChange(@RequestBody Dtos.CustomerChangeReq req, HttpServletRequest http) {
        return chain.changeCustomer(req, op(http));
    }

    @PostMapping("/demands")
    public CustomerDemand demand(@RequestBody Dtos.DemandReq req, HttpServletRequest http) {
        return chain.demand(req, op(http));
    }

    @PostMapping("/complaints")
    public Complaint complaint(@RequestBody Dtos.ComplaintReq req, HttpServletRequest http) {
        return chain.complaint(req, op(http));
    }

    @PostMapping("/bills/generate")
    public Bill generateBill(@RequestBody Map<String, Long> body, HttpServletRequest http) {
        return chain.generateBill(body.get("batchId"), op(http));
    }

    @GetMapping("/bills")
    public List<Bill> bills() {
        return billRepo.findAllByOrderByIdDesc();
    }

    @GetMapping("/bills/{id}")
    public Bill bill(@PathVariable Long id) {
        return billRepo.findById(id).orElseThrow(() -> new ApiException("账单不存在"));
    }

    @PostMapping("/bills/{id}/settle")
    public Bill settle(@PathVariable Long id, HttpServletRequest http) {
        return chain.settleBill(id, op(http));
    }
}
