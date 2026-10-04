package com.county.coldchain.service;

import com.county.coldchain.domain.DomainEnums.AlertLevel;
import com.county.coldchain.domain.DomainEnums.AlertType;
import com.county.coldchain.domain.DomainEnums.ComplaintType;
import com.county.coldchain.domain.DomainEnums.Responsibility;
import com.county.coldchain.domain.DomainEnums.Stage;
import com.county.coldchain.domain.entity.Complaint;
import com.county.coldchain.domain.entity.OutboundOrder;
import com.county.coldchain.repo.ComplaintRepository;
import com.county.coldchain.repo.OutboundOrderRepository;
import com.county.coldchain.support.BizException;
import com.county.coldchain.web.dto.ComplaintRequest;
import com.county.coldchain.web.dto.ComplaintResolveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 出库后客户投诉：坏果等问题回挂批次链，裁定赔付进入货主账单扣款。 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final OutboundOrderRepository outboundRepository;
    private final AlertService alertService;
    private final TraceService traceService;

    @Transactional
    public Complaint create(ComplaintRequest req) {
        OutboundOrder o = outboundRepository.findById(req.getOutboundId())
                .orElseThrow(() -> new BizException("出库单不存在"));
        Complaint c = new Complaint();
        c.setComplaintNo("C" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", complaintRepository.count() + 1));
        c.setOutboundId(o.getId());
        c.setBatchId(o.getBatchId());
        c.setCustomerId(o.getCustomerId());
        c.setType(ComplaintType.valueOf(req.getType()));
        c.setProblemWeight(req.getProblemWeight());
        c.setClaimAmount(req.getClaimAmount());
        c.setDescription(req.getDescription());
        c.setResponsibility(Responsibility.PENDING);
        c = complaintRepository.save(c);

        alertService.raise(AlertType.COMPLAINT, AlertLevel.CRITICAL, o.getBatchId(), null,
                "客户投诉 " + c.getComplaintNo(),
                "出库单 " + o.getOrderNo() + " 客户投诉：" + c.getType().desc
                        + (c.getProblemWeight() == null ? "" : "，问题重量 " + c.getProblemWeight() + "kg")
                        + (c.getClaimAmount() == null ? "" : "，索赔 " + c.getClaimAmount() + " 元")
                        + "。需追溯批次链并裁定责任。",
                "客户");
        traceService.log(o.getBatchId(), null, o.getCustomerId(), Stage.COMPLAINT,
                "客户投诉登记 " + c.getComplaintNo(),
                c.getType().desc + "，问题重量 " + c.getProblemWeight() + "kg，索赔 "
                        + c.getClaimAmount() + " 元。" + (c.getDescription() == null ? "" : c.getDescription()),
                "客服");
        return c;
    }

    @Transactional
    public Complaint resolve(Long id, ComplaintResolveRequest req) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new BizException("投诉不存在"));
        c.setResponsibility(Responsibility.valueOf(req.getResponsibility()));
        c.setSettledAmount(req.getSettledAmount());
        c.setHandler(req.getHandler() == null ? "管理端" : req.getHandler());
        c.setHandleResult(req.getHandleResult());
        c.setHandledAt(LocalDateTime.now());
        c.setResolved(true);
        complaintRepository.save(c);

        traceService.log(c.getBatchId(), null, c.getCustomerId(), Stage.COMPLAINT,
                "投诉裁定 " + c.getComplaintNo(),
                "责任方：" + c.getResponsibility().desc + "，裁定赔付 " + c.getSettledAmount()
                        + " 元。" + (c.getHandleResult() == null ? "" : c.getHandleResult())
                        + (c.getResponsibility() == Responsibility.WAREHOUSE
                        ? " 冷库承担的赔付将在货主账单中体现为投诉扣款（由冷库对客户赔付，不扣货主）。"
                        : c.getResponsibility() == Responsibility.OWNER
                        ? " 货主责任，赔付从货主货款中扣除。" : ""),
                c.getHandler());
        return c;
    }

    @Transactional(readOnly = true)
    public List<Complaint> list() {
        return complaintRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Complaint> byBatch(Long batchId) {
        return complaintRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
    }
}
