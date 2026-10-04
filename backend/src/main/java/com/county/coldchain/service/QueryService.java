package com.county.coldchain.service;

import com.county.coldchain.domain.*;
import com.county.coldchain.repo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 只读查询：批次详情、批次链、管理调度看板。
 * 看板服务于调度决策：高风险批次（临期/温区故障/温度超标）、已锁价客户、催货、可售重量变化。
 */
@Service
public class QueryService {

    private final BatchRepo batchRepo;
    private final OwnerRepo ownerRepo;
    private final CustomerRepo customerRepo;
    private final CategoryRepo categoryRepo;
    private final ZoneRepo zoneRepo;
    private final LotRepo lotRepo;
    private final QcRepo qcRepo;
    private final SortRepo sortRepo;
    private final OutboundRepo outboundRepo;
    private final LossRepo lossRepo;
    private final ComplaintRepo complaintRepo;
    private final DemandRepo demandRepo;
    private final PriceLockRepo priceLockRepo;
    private final BillRepo billRepo;
    private final EventRepo eventRepo;

    public QueryService(BatchRepo batchRepo, OwnerRepo ownerRepo, CustomerRepo customerRepo,
                        CategoryRepo categoryRepo, ZoneRepo zoneRepo, LotRepo lotRepo,
                        QcRepo qcRepo, SortRepo sortRepo, OutboundRepo outboundRepo,
                        LossRepo lossRepo, ComplaintRepo complaintRepo, DemandRepo demandRepo,
                        PriceLockRepo priceLockRepo, BillRepo billRepo, EventRepo eventRepo) {
        this.batchRepo = batchRepo;
        this.ownerRepo = ownerRepo;
        this.customerRepo = customerRepo;
        this.categoryRepo = categoryRepo;
        this.zoneRepo = zoneRepo;
        this.lotRepo = lotRepo;
        this.qcRepo = qcRepo;
        this.sortRepo = sortRepo;
        this.outboundRepo = outboundRepo;
        this.lossRepo = lossRepo;
        this.complaintRepo = complaintRepo;
        this.demandRepo = demandRepo;
        this.priceLockRepo = priceLockRepo;
        this.billRepo = billRepo;
        this.eventRepo = eventRepo;
    }

    public Map<String, Object> batchDetail(Long id) {
        Batch b = batchRepo.findById(id).orElseThrow();
        Map<String, Object> m = base(b);
        m.put("qc", qcRepo.findByBatchIdOrderByCheckedAtAsc(id));
        m.put("sorts", sortRepo.findByBatchIdOrderBySortedAtAsc(id));
        m.put("outbounds", outboundRepo.findByBatchIdOrderByOutboundAtAsc(id));
        m.put("losses", lossRepo.findByBatchIdOrderByLossAtAsc(id));
        m.put("complaints", complaintRepo.findByBatchIdOrderByComplainedAtAsc(id));
        m.put("demands", demandRepo.findByBatchIdOrderByCreatedAtDesc(id));
        m.put("lots", lotRepo.findByBatchIdOrderByIdAsc(id));
        m.put("events", eventRepo.findByBatchIdOrderBySeqAsc(id));
        m.put("bills", billRepo.findAllByOrderByIdDesc().stream().filter(x -> x.getBatch().getId().equals(id)).toList());
        return m;
    }

    private Map<String, Object> base(Batch b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("batchNo", b.getBatchNo());
        m.put("vehicleNo", b.getVehicleNo());
        m.put("owner", b.getOwner());
        m.put("category", b.getCategory());
        m.put("origin", b.getOrigin());
        m.put("arrivalWeight", b.getArrivalWeight());
        m.put("arrivalTemp", b.getArrivalTemp());
        m.put("tempExceeded", b.getArrivalTemp().compareTo(b.getCategory().getMaxArrivalTemp()) > 0);
        m.put("packaging", b.getPackaging());
        m.put("quarantineNo", b.getQuarantineNo());
        m.put("plannedCustomer", b.getPlannedCustomer());
        m.put("settleMethod", b.getSettleMethod());
        m.put("status", b.getStatus());
        m.put("mixedLoad", b.isMixedLoad());
        m.put("arrivalAt", b.getArrivalAt());
        m.put("inboundDate", b.getInboundDate());
        m.put("saleableWeight", b.getSaleableWeight());
        m.put("shippedWeight", b.getShippedWeight());
        m.put("lossWeight", b.getLossWeight());
        m.put("returnedWeight", b.getReturnedWeight());
        m.put("complaintDeductWeight", b.getComplaintDeductWeight());
        m.put("complaintDeductAmount", b.getComplaintDeductAmount());
        m.put("finalDestination", b.getFinalDestination());
        m.put("ownerAdvanceBalance", b.getOwner().getAdvanceBalance());
        return m;
    }

    /** 管理调度看板 */
    public Map<String, Object> dashboard() {
        List<Batch> batches = batchRepo.findAllByOrderByArrivalAtDesc();
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Batch b : batches) {
            Map<String, Object> row = base(b);
            long stayDays = b.getInboundDate() == null ? 0
                    : ChronoUnit.DAYS.between(b.getInboundDate(), today) + 1;
            row.put("stayDays", stayDays);
            int shelf = b.getCategory().getShelfLifeDays();
            row.put("shelfLifeDays", shelf);
            row.put("daysToExpiry", shelf - stayDays);

            List<StorageLot> inLots = lotRepo.findByBatchIdOrderByIdAsc(b.getId()).stream()
                    .filter(l -> l.getStatus() == Domain.LotStatus.IN_ZONE).toList();
            row.put("inZones", inLots.stream().map(l -> {
                Map<String, Object> z = new LinkedHashMap<>();
                z.put("zone", l.getZone().getName());
                z.put("faulty", l.getZone().isFaulty());
                z.put("grade", l.getGrade());
                z.put("weight", l.getWeight());
                z.put("location", l.getLocationCode());
                return z;
            }).toList());
            boolean zoneFault = inLots.stream().anyMatch(l -> l.getZone().isFaulty());
            row.put("zoneFault", zoneFault);

            List<CustomerDemand> openDemands = demandRepo.findByBatchIdOrderByCreatedAtDesc(b.getId()).stream()
                    .filter(d -> d.getStatus() == Domain.DemandStatus.OPEN).toList();
            row.put("openDemands", openDemands);
            row.put("urgentDemand", openDemands.stream().anyMatch(CustomerDemand::isPriority));

            // 是否存在有效锁价客户（计划客户或任何客户）
            boolean locked = false;
            if (b.getPlannedCustomer() != null) {
                for (Domain.Grade g : Domain.Grade.values()) {
                    if (!priceLockRepo.findActive(b.getPlannedCustomer().getId(), b.getCategory().getId(), g).isEmpty()) {
                        locked = true;
                        break;
                    }
                }
            }
            row.put("priceLocked", locked);

            // 高风险：温度超标 / 混批混装 / 临期（剩余货架期<=30%或<=1天）/ 温区故障 / 有投诉 / 损耗率>10%
            BigDecimal handled = b.getSaleableWeight().add(b.getShippedWeight())
                    .add(b.getLossWeight()).add(b.getReturnedWeight());
            BigDecimal lossRate = handled.signum() == 0 ? BigDecimal.ZERO
                    : b.getLossWeight().multiply(HUNDRED).divide(handled, 2, RoundingMode.HALF_UP);
            row.put("lossRate", lossRate);
            boolean nearExpiry = stayDays > 0 && (shelf - stayDays <= Math.max(1, shelf / 3));
            boolean highRisk = b.getArrivalTemp().compareTo(b.getCategory().getMaxArrivalTemp()) > 0
                    || b.isMixedLoad() || nearExpiry || zoneFault
                    || b.getComplaintDeductWeight().signum() > 0 || lossRate.compareTo(TEN) > 0;
            row.put("nearExpiry", nearExpiry);
            row.put("highRisk", highRisk);

            List<String> riskTags = new ArrayList<>();
            if (b.getArrivalTemp().compareTo(b.getCategory().getMaxArrivalTemp()) > 0) riskTags.add("到货温度超标");
            if (b.isMixedLoad()) riskTags.add("混批混装");
            if (nearExpiry) riskTags.add("临近货架期");
            if (zoneFault) riskTags.add("温区故障");
            if (!openDemands.isEmpty()) riskTags.add("客户催货");
            if (locked) riskTags.add("已锁价客户");
            if (lossRate.compareTo(TEN) > 0) riskTags.add("损耗率偏高");
            if (b.getComplaintDeductWeight().signum() > 0) riskTags.add("有坏果投诉");
            row.put("riskTags", riskTags);

            // 调度建议：先出高风险批次 / 先满足已锁价客户 / 转加工渠道
            List<String> suggestions = new ArrayList<>();
            if ((zoneFault || nearExpiry) && b.getSaleableWeight().signum() > 0) {
                suggestions.add("优先出库高风险批次");
            }
            if (locked && !openDemands.isEmpty()) suggestions.add("优先满足已锁价且催货客户");
            if (nearExpiry && b.getSaleableWeight().signum() > 0) suggestions.add("可临时转入加工渠道减损");
            if (zoneFault) suggestions.add("立即转仓至正常温区");
            row.put("suggestions", suggestions);
            rows.add(row);
        }

        // 调度排序：高风险 > 催货 > 锁价 > 停留天数
        rows.sort((x, y) -> {
            int c = Boolean.compare((boolean) y.get("highRisk"), (boolean) x.get("highRisk"));
            if (c != 0) return c;
            c = Boolean.compare((boolean) y.get("urgentDemand"), (boolean) x.get("urgentDemand"));
            if (c != 0) return c;
            c = Boolean.compare((boolean) y.get("priceLocked"), (boolean) x.get("priceLocked"));
            if (c != 0) return c;
            return Long.compare((long) y.get("stayDays"), (long) x.get("stayDays"));
        });

        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("batches", rows);
        dash.put("totalBatches", batches.size());
        dash.put("inStorage", batchRepo.countByStatus(Domain.BatchStatus.IN_STORAGE)
                + batchRepo.countByStatus(Domain.BatchStatus.PARTIAL_OUT));
        dash.put("sorting", batchRepo.countByStatus(Domain.BatchStatus.SORTING));
        dash.put("returned", batchRepo.countByStatus(Domain.BatchStatus.RETURNED));
        dash.put("shipped", batchRepo.countByStatus(Domain.BatchStatus.SHIPPED));
        dash.put("closed", batchRepo.countByStatus(Domain.BatchStatus.CLOSED));
        dash.put("highRiskCount", rows.stream().filter(r -> (boolean) r.get("highRisk")).count());
        dash.put("openDemands", demandRepo.findByStatusOrderByCreatedAtDesc(Domain.DemandStatus.OPEN)
                .stream().map(dm -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", dm.getId());
                    m.put("priority", dm.isPriority());
                    m.put("note", dm.getNote());
                    m.put("status", dm.getStatus());
                    m.put("createdAt", dm.getCreatedAt());
                    Map<String, Object> bm = new LinkedHashMap<>();
                    bm.put("id", dm.getBatch().getId());
                    bm.put("batchNo", dm.getBatch().getBatchNo());
                    m.put("batch", bm);
                    m.put("customer", dm.getCustomer());
                    return m;
                }).toList());
        dash.put("totalSaleableKg", batches.stream().map(Batch::getSaleableWeight).reduce(BigDecimal.ZERO, BigDecimal::add));
        dash.put("totalLossKg", batches.stream().map(Batch::getLossWeight).reduce(BigDecimal.ZERO, BigDecimal::add));
        dash.put("totalComplaintAmount", batches.stream().map(Batch::getComplaintDeductAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Map<String, Object>> zones = new ArrayList<>();
        for (Zone z : zoneRepo.findAllByOrderById()) {
            Map<String, Object> zm = new LinkedHashMap<>();
            zm.put("id", z.getId());
            zm.put("code", z.getCode());
            zm.put("name", z.getName());
            zm.put("zoneType", z.getZoneType());
            zm.put("targetTemp", z.getTargetTemp());
            zm.put("capacityKg", z.getCapacityKg());
            zm.put("usedKg", z.getUsedKg());
            zm.put("usageRate", z.getCapacityKg().signum() == 0 ? BigDecimal.ZERO
                    : z.getUsedKg().multiply(HUNDRED).divide(z.getCapacityKg(), 1, RoundingMode.HALF_UP));
            zm.put("faulty", z.isFaulty());
            zm.put("faultSince", z.getFaultSince());
            zm.put("faultNote", z.getFaultNote());
            zones.add(zm);
        }
        dash.put("zones", zones);
        return dash;
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TEN = new BigDecimal("10");

    public Map<String, Object> masterData() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("owners", ownerRepo.findAllByOrderById());
        m.put("customers", customerRepo.findAllByOrderById());
        m.put("categories", categoryRepo.findAllByOrderById());
        m.put("zones", zoneRepo.findAllByOrderById());
        m.put("priceLocks", priceLockRepo.findAllByOrderById());
        return m;
    }
}
