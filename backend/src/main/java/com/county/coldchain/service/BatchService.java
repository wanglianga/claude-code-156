package com.county.coldchain.service;

import com.county.coldchain.domain.DomainEnums.AlertLevel;
import com.county.coldchain.domain.DomainEnums.AlertType;
import com.county.coldchain.domain.DomainEnums.BatchStatus;
import com.county.coldchain.domain.DomainEnums.Stage;
import com.county.coldchain.domain.entity.Batch;
import com.county.coldchain.domain.entity.Category;
import com.county.coldchain.domain.entity.QuarantineInfo;
import com.county.coldchain.repo.BatchRepository;
import com.county.coldchain.repo.CategoryRepository;
import com.county.coldchain.repo.CustomerRepository;
import com.county.coldchain.repo.OwnerRepository;
import com.county.coldchain.support.BizException;
import com.county.coldchain.web.dto.ArrivalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 到货登记：批次链起点，并自动识别温度超标/混批混装/锁价/按原重结算等经营风险。 */
@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final OwnerRepository ownerRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final AlertService alertService;
    private final TraceService traceService;

    @Transactional
    public Batch register(ArrivalRequest req) {
        Category cat = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new BizException("品类不存在"));
        ownerRepository.findById(req.getOwnerId()).orElseThrow(() -> new BizException("货主不存在"));
        if (req.getPlannedCustomerId() != null) {
            customerRepository.findById(req.getPlannedCustomerId())
                    .orElseThrow(() -> new BizException("预计出库客户不存在"));
        }

        Batch b = new Batch();
        b.setBatchNo(generateBatchNo());
        b.setOwnerId(req.getOwnerId());
        b.setCategoryId(req.getCategoryId());
        b.setOrigin(req.getOrigin());
        b.setProductionDate(req.getProductionDate());
        b.setGrossWeight(req.getGrossWeight());
        b.setTareWeight(req.getTareWeight() == null ? BigDecimal.ZERO : req.getTareWeight());
        b.setNetWeight(req.getGrossWeight().subtract(b.getTareWeight()));
        b.setArrivalTemp(req.getArrivalTemp());
        b.setPackaging(req.getPackaging());
        QuarantineInfo q = new QuarantineInfo();
        q.setCertificateNo(req.getCertificateNo());
        q.setIssuingOrg(req.getIssuingOrg());
        q.setIssueDate(req.getCertIssueDate());
        q.setVerified(req.getCertificateVerified() == null ? false : req.getCertificateVerified());
        b.setQuarantine(q);
        b.setVehicleNo(req.getVehicleNo());
        b.setMixedLoad(Boolean.TRUE.equals(req.getMixedLoad()));
        b.setPlannedCustomerId(req.getPlannedCustomerId());
        b.setSettleMethod(com.county.coldchain.domain.DomainEnums.SettleMethod.valueOf(req.getSettleMethod()));
        b.setLockedPrice(req.getLockedPrice());
        b.setSettleByOriginalWeight(Boolean.TRUE.equals(req.getSettleByOriginalWeight()));
        b.setStatus(BatchStatus.REGISTERED);
        b.setArrivedAt(LocalDateTime.now());
        b = batchRepository.save(b);

        String op = req.getOperator() == null ? "系统" : req.getOperator();
        traceService.log(b.getId(), Stage.ARRIVAL, "到货登记 " + b.getBatchNo(),
                String.format("%s，净重 %s kg，到货温度 %s℃，包装：%s，检疫证：%s（%s），预计客户ID：%s，结算：%s",
                        cat.getName(), b.getNetWeight(), b.getArrivalTemp(), b.getPackaging(),
                        q.getCertificateNo(), Boolean.TRUE.equals(q.getVerified()) ? "已核验" : "未核验",
                        b.getPlannedCustomerId(), b.getSettleMethod().desc),
                op);

        // ---- 自动风控识别 ----
        if (cat.getMaxArrivalTemp() != null && b.getArrivalTemp().compareTo(cat.getMaxArrivalTemp()) > 0
                || cat.getMinArrivalTemp() != null && b.getArrivalTemp().compareTo(cat.getMinArrivalTemp()) < 0) {
            alertService.raise(AlertType.TEMP_EXCEEDED, AlertLevel.CRITICAL, b.getId(), null,
                    "到货温度超标 " + b.getBatchNo(),
                    String.format("实测 %s℃，%s 允许区间 [%s, %s]℃",
                            b.getArrivalTemp(), cat.getName(), cat.getMinArrivalTemp(), cat.getMaxArrivalTemp()),
                    op);
        }
        if (b.getMixedLoad()) {
            alertService.raise(AlertType.MIXED_LOAD, AlertLevel.WARN, b.getId(), null,
                    "混批混装 " + b.getBatchNo(),
                    "车牌 " + b.getVehicleNo() + " 存在多货主/多品类混装，需分拣时拆分清楚，防止交叉污染与错批。",
                    op);
        }
        if (b.getSettleByOriginalWeight()) {
            alertService.raise(AlertType.ORIGINAL_WEIGHT_SETTLE, AlertLevel.WARN, b.getId(), null,
                    "货主要求按原重结算 " + b.getBatchNo(),
                    "货主要求按到货净重 " + b.getNetWeight() + "kg 结算，分拣损耗可能由冷库承担，财务账单将按原重补差。",
                    op);
        }
        if (Boolean.FALSE.equals(q.getVerified())) {
            alertService.raise(AlertType.MIXED_LOAD, AlertLevel.WARN, b.getId(), null,
                    "票证未核验 " + b.getBatchNo(),
                    "检疫证明 " + q.getCertificateNo() + " 未核验通过，质检验证前不得入库。", op);
        }
        return b;
    }

    @Transactional(readOnly = true)
    public List<Batch> list(List<BatchStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return batchRepository.findAll();
        }
        return batchRepository.findByStatusIn(statuses);
    }

    @Transactional(readOnly = true)
    public Batch get(Long id) {
        return batchRepository.findById(id).orElseThrow(() -> new BizException("批次不存在: " + id));
    }

    /** 同车批次（混批混装核查） */
    @Transactional(readOnly = true)
    public List<Batch> sameVehicle(String vehicleNo) {
        return batchRepository.findByVehicleNo(vehicleNo);
    }

    private String generateBatchNo() {
        return "B" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", batchRepository.maxId() + 1);
    }
}
