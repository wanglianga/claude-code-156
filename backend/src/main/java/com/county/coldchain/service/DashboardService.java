package com.county.coldchain.service;

import com.county.coldchain.domain.DomainEnums.AlertLevel;
import com.county.coldchain.domain.DomainEnums.BatchStatus;
import com.county.coldchain.domain.DomainEnums.LockStatus;
import com.county.coldchain.domain.entity.Alert;
import com.county.coldchain.domain.entity.Batch;
import com.county.coldchain.domain.entity.Category;
import com.county.coldchain.domain.entity.Customer;
import com.county.coldchain.domain.entity.OutboundOrder;
import com.county.coldchain.domain.entity.PriceLock;
import com.county.coldchain.domain.entity.StockLot;
import com.county.coldchain.domain.entity.Zone;
import com.county.coldchain.repo.AlertRepository;
import com.county.coldchain.repo.BatchRepository;
import com.county.coldchain.repo.CategoryRepository;
import com.county.coldchain.repo.CustomerRepository;
import com.county.coldchain.repo.OutboundOrderRepository;
import com.county.coldchain.repo.PriceLockRepository;
import com.county.coldchain.repo.StockLotRepository;
import com.county.coldchain.repo.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端看板：在仓天数、温区占用、客户催货、损耗责任、可售重量变化，
 * 并给出调度建议：先出高风险批次、先满足锁价客户、转入加工渠道。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BatchRepository batchRepository;
    private final StockLotRepository lotRepository;
    private final ZoneRepository zoneRepository;
    private final AlertRepository alertRepository;
    private final OutboundOrderRepository outboundRepository;
    private final PriceLockRepository priceLockRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Batch> batches = batchRepository.findAll();
        Map<BatchStatus, Long> statusCount = new LinkedHashMap<>();
        for (BatchStatus s : BatchStatus.values()) {
            statusCount.put(s, batches.stream().filter(b -> b.getStatus() == s).count());
        }
        m.put("batchStatusCount", statusCount);
        m.put("totalBatches", batches.size());

        BigDecimal inStoreWeight = lotRepository.findByStatus("IN").stream()
                .map(StockLot::getCurrentWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("inStoreWeight", inStoreWeight);
        m.put("openAlerts", alertRepository.findByResolvedFalseOrderByCreatedAtDesc().size());
        m.put("criticalAlerts", alertRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .filter(a -> a.getLevel() == AlertLevel.CRITICAL).count());

        List<OutboundOrder> planned = outboundRepository.findByStatus(
                com.county.coldchain.domain.DomainEnums.OrderStatus.PLANNED);
        m.put("plannedOutboundOrders", planned.size());
        m.put("plannedOutboundWeight", planned.stream()
                .map(OutboundOrder::getTotalWeight).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("priorityUrges", planned.stream().filter(OutboundOrder::getPriority).count());

        m.put("activeLocks", priceLockRepository.findByStatus(LockStatus.ACTIVE).size());
        return m;
    }

    /** 在仓批次监控行：停留天数、温区、等级可售重量、保鲜剩余、告警、锁价客户、催货。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> storageMonitor() {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<StockLot> inLots = lotRepository.findByStatus("IN");
        Map<Long, List<StockLot>> byBatch = new HashMap<>();
        inLots.forEach(l -> byBatch.computeIfAbsent(l.getBatchId(), k -> new ArrayList<>()).add(l));

        for (Map.Entry<Long, List<StockLot>> e : byBatch.entrySet()) {
            Batch b = batchRepository.findById(e.getKey()).orElse(null);
            if (b == null) {
                continue;
            }
            Category cat = categoryRepository.findById(b.getCategoryId()).orElse(null);
            List<StockLot> lots = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batchId", b.getId());
            row.put("batchNo", b.getBatchNo());
            row.put("category", cat == null ? "" : cat.getName());
            long stayDays = lots.stream().mapToLong(l ->
                    Math.max(0, Duration.between(l.getInAt(), LocalDateTime.now()).toDays())).max().orElse(0);
            row.put("stayDays", stayDays);
            row.put("netWeight", b.getNetWeight());
            row.put("sellableWeight", lots.stream().map(StockLot::getCurrentWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("lossWeight", b.getLossWeight());
            row.put("outWeight", b.getOutWeight());
            row.put("returnedWeight", b.getReturnedWeight());

            // 按等级分组重量与温区
            List<Map<String, Object>> gradeRows = new ArrayList<>();
            for (StockLot l : lots) {
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("grade", l.getGrade().name());
                g.put("weight", l.getCurrentWeight());
                g.put("lotNo", l.getLotNo());
                Zone z = zoneRepository.findById(l.getZoneId()).orElse(null);
                g.put("zone", z == null ? null : z.getName());
                g.put("zoneFault", z != null && Boolean.TRUE.equals(z.getFault()));
                long lotDays = Math.max(0, Duration.between(l.getInAt(), LocalDateTime.now()).toDays());
                g.put("stayDays", lotDays);
                g.put("shelfLifeDays", l.getShelfLifeDays());
                g.put("shelfLeftDays", l.getShelfLifeDays() == null ? null
                        : l.getShelfLifeDays() - lotDays);
                gradeRows.add(g);
            }
            row.put("grades", gradeRows);

            List<Alert> alerts = alertRepository.findByBatchIdOrderByCreatedAtDesc(b.getId()).stream()
                    .filter(a -> !a.getResolved()).toList();
            row.put("openAlerts", alerts.stream().map(a -> Map.of(
                    "type", a.getType().name(), "level", a.getLevel().name(),
                    "title", a.getTitle())).toList());
            row.put("risk", alerts.stream().anyMatch(a -> a.getLevel() == AlertLevel.CRITICAL));

            // 客户催货（优先排单）
            List<OutboundOrder> urges = outboundRepository.findByStatus(
                            com.county.coldchain.domain.DomainEnums.OrderStatus.PLANNED).stream()
                    .filter(o -> o.getBatchId().equals(b.getId())).toList();
            if (!urges.isEmpty()) {
                Customer c = customerRepository.findById(urges.get(0).getCustomerId()).orElse(null);
                row.put("urgentCustomer", c == null ? "" : c.getName());
                row.put("priority", urges.stream().anyMatch(OutboundOrder::getPriority));
            }
            // 锁价客户
            if (b.getPlannedCustomerId() != null) {
                Customer c = customerRepository.findById(b.getPlannedCustomerId()).orElse(null);
                row.put("plannedCustomer", c == null ? "" : c.getName());
            }
            row.put("lockedPrice", b.getLockedPrice());
            rows.add(row);
        }
        rows.sort(Comparator
                .comparing((Map<String, Object> r) -> (boolean) r.getOrDefault("risk", false)).reversed()
                .thenComparing(r -> (long) r.getOrDefault("stayDays", 0L), Comparator.reverseOrder()));
        return rows;
    }

    /** 调度建议：综合温区故障/温度超标/保鲜临界/锁价履约/加工渠道。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> dispatchSuggestions() {
        List<Map<String, Object>> tips = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 高风险批次（温区故障、温度超标、投诉、保鲜临界）
        for (StockLot lot : lotRepository.findByStatus("IN")) {
            Batch b = batchRepository.findById(lot.getBatchId()).orElse(null);
            if (b == null) {
                continue;
            }
            Zone zone = zoneRepository.findById(lot.getZoneId()).orElse(null);
            long days = Math.max(0, Duration.between(lot.getInAt(), now).toDays());
            boolean zoneFault = zone != null && Boolean.TRUE.equals(zone.getFault());
            boolean shelfCritical = lot.getShelfLifeDays() != null
                    && days >= Math.round(lot.getShelfLifeDays() * 0.7);
            boolean complained = alertRepository.findByBatchIdOrderByCreatedAtDesc(b.getId()).stream()
                    .anyMatch(a -> !a.getResolved() && a.getLevel() == AlertLevel.CRITICAL);
            if (zoneFault || shelfCritical || complained) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("action", "URGENT_OUT");
                t.put("batchId", b.getId());
                t.put("batchNo", b.getBatchNo());
                t.put("lotNo", lot.getLotNo());
                t.put("grade", lot.getGrade().name());
                t.put("weight", lot.getCurrentWeight());
                t.put("reason", zoneFault ? "所在温区故障，建议立即出库或转仓"
                        : shelfCritical ? "保鲜期临界（已在仓 " + days + " 天/" + lot.getShelfLifeDays() + " 天）"
                        : "批次存在严重未处理告警，优先出库降低赔付风险");
                tips.add(t);
            }
            // 2. 加工级或在仓过久 → 加工渠道
            boolean longStay = lot.getShelfLifeDays() != null && days >= lot.getShelfLifeDays();
            if ((lot.getGrade() == com.county.coldchain.domain.DomainEnums.Grade.PROCESS && days >= 1)
                    || longStay) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("action", "TO_PROCESSING");
                t.put("batchId", b.getId());
                t.put("batchNo", b.getBatchNo());
                t.put("lotNo", lot.getLotNo());
                t.put("weight", lot.getCurrentWeight());
                t.put("reason", longStay ? "已达/超保鲜天数，建议转加工渠道止损"
                        : "加工级货品，建议优先对接加工渠道客户");
                tips.add(t);
            }
        }

        // 3. 锁价客户待履约：找同品类在库货，建议优先配货
        for (PriceLock lock : priceLockRepository.findByStatus(LockStatus.ACTIVE)) {
            BigDecimal remain = lock.getLockedQty().subtract(lock.getFulfilledQty());
            if (remain.signum() <= 0) {
                continue;
            }
            BigDecimal available = BigDecimal.ZERO;
            List<String> lotNos = new ArrayList<>();
            for (StockLot lot : lotRepository.findByStatus("IN")) {
                Batch b = batchRepository.findById(lot.getBatchId()).orElse(null);
                if (b == null) {
                    continue;
                }
                Category cat = categoryRepository.findById(b.getCategoryId()).orElse(null);
                if (cat != null && cat.getCode().equals(lock.getCategoryCode())) {
                    available = available.add(lot.getCurrentWeight());
                    lotNos.add(lot.getLotNo());
                }
            }
            if (available.signum() > 0) {
                Customer c = customerRepository.findById(lock.getCustomerId()).orElse(null);
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("action", "LOCK_FULFILL");
                t.put("customer", c == null ? "" : c.getName());
                t.put("categoryCode", lock.getCategoryCode());
                t.put("lockedQtyRemain", remain);
                t.put("availableWeight", available);
                t.put("lots", lotNos);
                t.put("reason", "锁价客户待履约 " + remain + "kg，当前同品类在库 " + available
                        + "kg，应优先满足锁价客户");
                tips.add(t);
            }
        }
        return tips;
    }

    /** 温区占用看板。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> zones() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Zone z : zoneRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", z.getId());
            m.put("code", z.getCode());
            m.put("name", z.getName());
            m.put("type", z.getType().name());
            m.put("targetTemp", z.getTargetTemp());
            m.put("currentTemp", z.getCurrentTemp());
            m.put("fault", z.getFault());
            m.put("faultNote", z.getFaultNote());
            m.put("capacityKg", z.getCapacityKg());
            m.put("occupiedKg", z.getOccupiedKg());
            m.put("usageRate", z.getCapacityKg().signum() == 0 ? 0
                    : z.getOccupiedKg().divide(z.getCapacityKg(), 4, java.math.RoundingMode.HALF_UP));
            long lots = lotRepository.findByZoneIdAndStatus(z.getId(), "IN").size();
            m.put("lotCount", lots);
            list.add(m);
        }
        return list;
    }
}
