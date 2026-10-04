package com.county.coldchain.service;

import com.county.coldchain.domain.*;
import com.county.coldchain.repo.*;
import com.county.coldchain.web.ApiException;
import com.county.coldchain.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 批次链核心服务：把入库、质检、分拣、库位、出库、损耗、结算连成一条链。
 * 所有写动作同步追加 TraceEvent；任何降级/转仓/报损都会改变后续账单。
 */
@Service
public class BatchChainService {

    private final BatchRepo batchRepo;
    private final OwnerRepo ownerRepo;
    private final CustomerRepo customerRepo;
    private final CategoryRepo categoryRepo;
    private final QcRepo qcRepo;
    private final SortRepo sortRepo;
    private final ZoneRepo zoneRepo;
    private final LotRepo lotRepo;
    private final OutboundRepo outboundRepo;
    private final LossRepo lossRepo;
    private final ComplaintRepo complaintRepo;
    private final DemandRepo demandRepo;
    private final PriceLockRepo priceLockRepo;
    private final BillRepo billRepo;
    private final TraceService trace;

    public BatchChainService(BatchRepo batchRepo, OwnerRepo ownerRepo,
                             CustomerRepo customerRepo, CategoryRepo categoryRepo,
                             QcRepo qcRepo, SortRepo sortRepo, ZoneRepo zoneRepo,
                             LotRepo lotRepo, OutboundRepo outboundRepo,
                             LossRepo lossRepo, ComplaintRepo complaintRepo,
                             DemandRepo demandRepo, PriceLockRepo priceLockRepo,
                             BillRepo billRepo, TraceService trace) {
        this.batchRepo = batchRepo;
        this.ownerRepo = ownerRepo;
        this.customerRepo = customerRepo;
        this.categoryRepo = categoryRepo;
        this.qcRepo = qcRepo;
        this.sortRepo = sortRepo;
        this.zoneRepo = zoneRepo;
        this.lotRepo = lotRepo;
        this.outboundRepo = outboundRepo;
        this.lossRepo = lossRepo;
        this.complaintRepo = complaintRepo;
        this.demandRepo = demandRepo;
        this.priceLockRepo = priceLockRepo;
        this.billRepo = billRepo;
        this.trace = trace;
    }

    private static final BigDecimal TON = new BigDecimal("1000");

    // ---------------- 到货登记 ----------------

    @Transactional
    public Batch register(Dtos.RegisterReq req, String operator) {
        Owner owner = ownerRepo.findById(req.ownerId()).orElseThrow(() -> new ApiException("货主不存在"));
        Category cat = categoryRepo.findById(req.categoryId()).orElseThrow(() -> new ApiException("品类不存在"));
        Customer customer = req.plannedCustomerId() == null ? null
                : customerRepo.findById(req.plannedCustomerId()).orElseThrow(() -> new ApiException("预计客户不存在"));
        if (req.arrivalWeight() == null || req.arrivalWeight().signum() <= 0) {
            throw new ApiException("到货重量必须大于0");
        }
        Batch b = new Batch();
        b.setBatchNo(generateBatchNo());
        b.setVehicleNo(req.vehicleNo());
        b.setOwner(owner);
        b.setCategory(cat);
        b.setOrigin(req.origin());
        b.setArrivalWeight(req.arrivalWeight());
        b.setArrivalTemp(req.arrivalTemp());
        b.setPackaging(req.packaging());
        b.setQuarantineNo(req.quarantineNo());
        b.setPlannedCustomer(customer);
        b.setSettleMethod(req.settleMethod() == null ? Domain.SettleMethod.ACTUAL : req.settleMethod());
        b.setMixedLoad(Boolean.TRUE.equals(req.mixedLoad()));
        b.setArrivalAt(LocalDateTime.now());
        b = batchRepo.save(b);

        String detail = String.format("到货登记：货主=%s，品类=%s，产地=%s，原重=%skg，到货温度=%s℃，包装=%s，检疫证=%s，预计客户=%s，结算=%s%s",
                owner.getName(), cat.getName(), b.getOrigin(), b.getArrivalWeight(), b.getArrivalTemp(),
                b.getPackaging(), b.getQuarantineNo(), customer == null ? "未指定" : customer.getName(),
                b.getSettleMethod().label, b.isMixedLoad() ? "，⚠混批混装" : "");
        trace.record(b, Domain.EventType.REGISTER, detail, operator);
        return b;
    }

    private String generateBatchNo() {
        String day = LocalDate.now().toString().replace("-", "");
        return "P" + day + "-" + String.format("%03d", batchRepo.count() + 1);
    }

    // ---------------- 质检验收 ----------------

    @Transactional
    public QcRecord qc(Dtos.QcReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        if (b.getStatus() != Domain.BatchStatus.REGISTERED) {
            throw new ApiException("当前批次状态为「" + b.getStatus().label + "」，不能重复质检");
        }
        Category cat = b.getCategory();
        QcRecord q = new QcRecord();
        q.setBatch(b);
        q.setInspector(operator);
        q.setCheckedAt(LocalDateTime.now());
        q.setAppearanceScore(req.appearanceScore());
        q.setMaturityScore(req.maturityScore());
        q.setRotRate(nz(req.rotRate()));
        q.setMoistureRate(nz(req.moistureRate()));
        q.setMeasuredTemp(req.measuredTemp());
        q.setDocumentsOk(Boolean.TRUE.equals(req.documentsOk()));
        q.setTempExceeded(req.measuredTemp() != null && req.measuredTemp().compareTo(cat.getMaxArrivalTemp()) > 0);
        q.setGrade(req.grade());
        q.setResult(req.result());
        q.setRemark(req.remark());

        BigDecimal accepted;
        BigDecimal returned;
        if (req.result() == Domain.QcResult.RETURN) {
            // 退货：默认整车退；也允许部分接受（acceptedWeight > 0）
            accepted = req.acceptedWeight() == null ? BigDecimal.ZERO : req.acceptedWeight();
            if (accepted.compareTo(b.getArrivalWeight()) > 0) {
                throw new ApiException("验收重量不能大于到货原重");
            }
            returned = b.getArrivalWeight().subtract(accepted);
        } else {
            accepted = req.acceptedWeight() == null ? b.getArrivalWeight() : req.acceptedWeight();
            if (accepted.compareTo(b.getArrivalWeight()) > 0) {
                throw new ApiException("验收重量不能大于到货原重");
            }
            returned = b.getArrivalWeight().subtract(accepted);
        }
        q.setAcceptedWeight(accepted);
        q.setReturnWeight(returned);
        q = qcRepo.save(q);

        StringBuilder detail = new StringBuilder(String.format(
                "质检验收：外观=%d分 成熟度=%d分 腐损率=%s%% 含水率=%s%% 测温=%s℃（限值%s℃%s）票证=%s → 结论=%s，定级=%s，接受=%skg，退货=%skg",
                nzInt(q.getAppearanceScore()), nzInt(q.getMaturityScore()), q.getRotRate(), q.getMoistureRate(),
                q.getMeasuredTemp(), cat.getMaxArrivalTemp(), q.isTempExceeded() ? "，⚠超标" : "，合格",
                q.isDocumentsOk() ? "齐全" : "⚠缺失", req.result().label,
                req.grade() == null ? "-" : req.grade().label, accepted, returned));
        if (b.isMixedLoad()) {
            detail.append("；⚠本车混批混装");
        }
        if (req.remark() != null && !req.remark().isBlank()) {
            detail.append("；备注：").append(req.remark());
        }

        if (req.result() == Domain.QcResult.RETURN && accepted.signum() == 0) {
            b.setStatus(Domain.BatchStatus.RETURNED);
            b.setReturnedWeight(returned);
            b.setInboundDate(LocalDate.now());
            trace.record(b, Domain.EventType.RETURN, "整车退货：" + detail, operator);
        } else {
            b.setReturnedWeight(returned);
            b.setInboundDate(LocalDate.now());
            if (req.result() == Domain.QcResult.SORT) {
                b.setStatus(Domain.BatchStatus.SORTING);
                b.setSaleableWeight(accepted);
            } else {
                // 入库 / 降级入库：直接按定级落位温区
                if (req.grade() == null) {
                    throw new ApiException("入库结论必须给出质量等级");
                }
                allocateLot(b, req.grade(), accepted, LocalDate.now(), null);
                b.setStatus(Domain.BatchStatus.IN_STORAGE);
                b.setSaleableWeight(accepted);
                detail.append("；已分配保鲜温区：").append(cat.getTargetZoneType());
            }
            trace.record(b, Domain.EventType.QC, detail.toString(), operator);
        }
        return q;
    }

    // ---------------- 分拣 ----------------

    @Transactional
    public SortRecord sort(Dtos.SortReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        if (b.getStatus() != Domain.BatchStatus.SORTING && b.getStatus() != Domain.BatchStatus.IN_STORAGE) {
            throw new ApiException("当前状态「" + b.getStatus().label + "」不能分拣");
        }
        BigDecimal input = req.inputWeight() == null ? b.getSaleableWeight() : req.inputWeight();
        BigDecimal a = nz(req.weightA()), cw = nz(req.weightB()), cg = nz(req.weightC());
        BigDecimal output = a.add(cw).add(cg);
        if (output.compareTo(input) > 0) {
            throw new ApiException("分级产出合计 " + output + "kg 超过投线重量 " + input + "kg");
        }
        BigDecimal loss = input.subtract(output);

        SortRecord s = new SortRecord();
        s.setBatch(b);
        s.setOperator(operator);
        s.setSortedAt(LocalDateTime.now());
        s.setInputWeight(input);
        s.setWeightA(a);
        s.setWeightB(cw);
        s.setWeightC(cg);
        s.setLossWeight(loss);
        s.setLaborHours(nz(req.laborHours()));
        s.setLaborRate(nz(req.laborRate()));
        s.setLossResponsible(req.lossResponsible() == null ? Domain.Responsible.OWNER : req.lossResponsible());
        s.setDisputed(Boolean.TRUE.equals(req.disputed()));
        s.setDisputeNote(req.disputeNote());
        s.setRemark(req.remark());
        s = sortRepo.save(s);

        // 待分拣批次尚无库位，直接按等级落位；追加分拣（在仓）时新增库位并入账可售重量
        LocalDate today = LocalDate.now();
        if (a.signum() > 0) allocateLot(b, Domain.Grade.A, a, today, null);
        if (cw.signum() > 0) allocateLot(b, Domain.Grade.B, cw, today, null);
        if (cg.signum() > 0) allocateLot(b, Domain.Grade.C, cg, today, null);

        b.setStatus(Domain.BatchStatus.IN_STORAGE);
        // 可售重量 = 现存开放库位重量合计（保证与库位一致）
        b.setSaleableWeight(openLotWeight(b.getId()));
        b.setLossWeight(nz(b.getLossWeight()).add(loss));
        batchRepo.save(b);

        StringBuilder detail = new StringBuilder(String.format(
                "分拣：投线=%skg → A=%s B=%s C=%s，损耗=%skg（损耗率%s%%），责任=%s，工时=%s×%s元",
                input, a, cw, cg, loss,
                input.signum() == 0 ? "0" : loss.multiply(HUNDRED).divide(input, 2, RoundingMode.HALF_UP),
                s.getLossResponsible().label, s.getLaborHours(), s.getLaborRate()));
        if (s.isDisputed()) {
            detail.append("；⚠分拣损耗争议：").append(s.getDisputeNote() == null ? "" : s.getDisputeNote());
        }
        trace.record(b, Domain.EventType.SORT, detail.toString(), operator);
        return s;
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // ---------------- 报损 ----------------

    @Transactional
    public LossReportView reportLoss(Dtos.LossReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        StorageLot lot = null;
        if (req.lotId() != null) {
            lot = lotRepo.findById(req.lotId()).orElseThrow(() -> new ApiException("库位不存在"));
            if (!lot.getBatch().getId().equals(b.getId()) || lot.getStatus() != Domain.LotStatus.IN_ZONE) {
                throw new ApiException("库位不属于该批次或已不在库");
            }
            if (req.weight().compareTo(lot.getWeight()) > 0) {
                throw new ApiException("报损重量超过库位现存 " + lot.getWeight() + "kg");
            }
            lot.setWeight(lot.getWeight().subtract(req.weight()));
            Zone z = lot.getZone();
            z.setUsedKg(nz(z.getUsedKg()).subtract(req.weight()));
            if (lot.getWeight().signum() == 0) {
                lot.setStatus(Domain.LotStatus.SCRAPPED);
                lot.setClosedAt(LocalDate.now());
            }
            lotRepo.save(lot);
        }
        LossRecord l = new LossRecord();
        l.setBatch(b);
        l.setLot(lot);
        l.setGrade(req.grade());
        l.setWeight(req.weight());
        l.setReason(req.reason());
        l.setResponsible(req.responsible() == null ? Domain.Responsible.OWNER : req.responsible());
        l.setLossAt(LocalDateTime.now());
        l.setOperator(operator);
        lossRepo.save(l);

        b.setLossWeight(nz(b.getLossWeight()).add(req.weight()));
        b.setSaleableWeight(openLotWeight(b.getId()));
        refreshBatchStockStatus(b);
        trace.record(b, Domain.EventType.LOSS, String.format(
                "报损：等级=%s，重量=%skg，原因=%s，责任=%s，库位=%s，当前可售=%skg",
                req.grade(), req.weight(), req.reason(), l.getResponsible().label,
                lot == null ? "-" : lot.getLocationCode(), b.getSaleableWeight()), operator);
        return new LossReportView(l, lot);
    }

    public record LossReportView(LossRecord loss, StorageLot lot) {}

    // ---------------- 出库 ----------------

    @Transactional
    public Outbound outbound(Dtos.OutboundReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        Customer customer = customerRepo.findById(req.customerId())
                .orElseThrow(() -> new ApiException("客户不存在"));
        if (b.getStatus() != Domain.BatchStatus.IN_STORAGE && b.getStatus() != Domain.BatchStatus.PARTIAL_OUT) {
            throw new ApiException("批次当前状态「" + b.getStatus().label + "」不能出库");
        }
        List<StorageLot> lots = lotRepo.findByBatchIdOrderByIdAsc(b.getId()).stream()
                .filter(l -> l.getStatus() == Domain.LotStatus.IN_ZONE && l.getGrade() == req.grade())
                .sorted(Comparator.comparing(StorageLot::getStorageFrom))
                .toList();
        BigDecimal available = lots.stream().map(StorageLot::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (req.weight().compareTo(available) > 0) {
            throw new ApiException(String.format("%s级在库仅 %skg，不足以出库 %skg", req.grade(), available, req.weight()));
        }

        // 定价：指定锁价 > 客户有效锁价 > 品类等级参考价
        BigDecimal price;
        PriceLock lock = null;
        if (req.priceLockId() != null) {
            lock = priceLockRepo.findById(req.priceLockId()).orElseThrow(() -> new ApiException("锁价单不存在"));
            price = lock.getLockedPrice();
        } else {
            List<PriceLock> active = priceLockRepo.findActive(customer.getId(), b.getCategory().getId(), req.grade());
            if (!active.isEmpty()) {
                lock = active.get(0);
                price = lock.getLockedPrice();
            } else {
                price = gradePrice(b.getCategory(), req.grade());
            }
        }

        // FIFO 扣减库位
        BigDecimal remain = req.weight();
        List<String> usedLocations = new ArrayList<>();
        for (StorageLot lot : lots) {
            if (remain.signum() == 0) break;
            BigDecimal take = lot.getWeight().min(remain);
            lot.setWeight(lot.getWeight().subtract(take));
            Zone z = lot.getZone();
            z.setUsedKg(nz(z.getUsedKg()).subtract(take));
            remain = remain.subtract(take);
            usedLocations.add(lot.getLocationCode() + "(" + take + "kg)");
            if (lot.getWeight().signum() == 0) {
                lot.setStatus(Domain.LotStatus.OUT);
                lot.setClosedAt(LocalDate.now());
            }
            lotRepo.save(lot);
        }

        Outbound o = new Outbound();
        o.setBatch(b);
        o.setCustomer(customer);
        o.setGrade(req.grade());
        o.setWeight(req.weight());
        o.setUnitPrice(price);
        o.setAmount(price.multiply(req.weight()).setScale(2, RoundingMode.HALF_UP));
        o.setChannel(req.channel() == null ? Domain.Channel.NORMAL : req.channel());
        o.setPriority(Boolean.TRUE.equals(req.priority()));
        o.setCustomerChanged(Boolean.TRUE.equals(req.customerChanged()));
        o.setPriceLock(lock);
        o.setOutboundAt(LocalDateTime.now());
        o.setOperator(operator);
        o.setRemark(req.remark());
        o = outboundRepo.save(o);

        b.setShippedWeight(nz(b.getShippedWeight()).add(req.weight()));
        b.setSaleableWeight(openLotWeight(b.getId()));
        // 最终流向按全部出库客户（含加工渠道）去重汇总，供监管追溯
        b.setFinalDestination(outboundRepo.findByBatchIdOrderByOutboundAtAsc(b.getId()).stream()
                .map(x -> x.getCustomer().getName() + "（" + x.getChannel().label + "）")
                .distinct().reduce((x, y) -> x + "、" + y).orElse(null));
        refreshBatchStockStatus(b);

        boolean actualChange = Boolean.TRUE.equals(req.customerChanged()) && b.getPlannedCustomer() != null
                && !b.getPlannedCustomer().getId().equals(customer.getId());
        if (actualChange) {
            String orig = b.getPlannedCustomer().getName();
            trace.record(b, Domain.EventType.CUSTOMER_CHANGE, String.format(
                    "出库时临时改客户：原计划=%s → 实际=%s，重量=%skg，等级=%s", orig, customer.getName(), req.weight(), req.grade()), operator);
            o.setCustomerChanged(true);
        }
        // 满足该批次该客户的催货
        for (CustomerDemand d : demandRepo.findByBatchIdOrderByCreatedAtDesc(b.getId())) {
            if (d.getStatus() == Domain.DemandStatus.OPEN && d.getCustomer().getId().equals(customer.getId())) {
                d.setStatus(Domain.DemandStatus.FULFILLED);
                d.setFulfilledAt(LocalDateTime.now());
                demandRepo.save(d);
            }
        }

        trace.record(b, Domain.EventType.OUTBOUND, String.format(
                "出库：客户=%s，等级=%s，重量=%skg，单价=%s元/kg，金额=%s元，渠道=%s%s，锁价=%s，库位=%s",
                customer.getName(), req.grade(), req.weight(), price, o.getAmount(), o.getChannel().label,
                o.isPriority() ? "，⚠优先出库" : "", lock == null ? "无（参考价）" : "有",
                String.join("、", usedLocations)), operator);
        return o;
    }

    // ---------------- 转仓（温区故障等） ----------------

    @Transactional
    public List<StorageLot> transfer(Dtos.TransferReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        Zone to = zoneRepo.findById(req.toZoneId()).orElseThrow(() -> new ApiException("目标温区不存在"));
        if (to.isFaulty()) {
            throw new ApiException("目标温区「" + to.getName() + "」处于故障状态，不能转入");
        }
        List<StorageLot> open = lotRepo.findByBatchIdOrderByIdAsc(b.getId()).stream()
                .filter(l -> l.getStatus() == Domain.LotStatus.IN_ZONE).toList();
        if (open.isEmpty()) {
            throw new ApiException("该批次没有在库库位");
        }
        LocalDate today = LocalDate.now();
        List<StorageLot> created = new ArrayList<>();
        List<String> moveDesc = new ArrayList<>();
        for (StorageLot old : open) {
            Zone from = old.getZone();
            if (from.getId().equals(to.getId())) continue;
            if (to.getCapacityKg().subtract(nz(to.getUsedKg())).compareTo(old.getWeight()) < 0) {
                throw new ApiException("目标温区容量不足（需要 " + old.getWeight() + "kg）");
            }
            from.setUsedKg(nz(from.getUsedKg()).subtract(old.getWeight()));
            old.setStatus(Domain.LotStatus.TRANSFERRED);
            old.setClosedAt(today);
            lotRepo.save(old);

            StorageLot nw = new StorageLot();
            nw.setBatch(b);
            nw.setZone(to);
            nw.setLocationCode(to.getCode() + "-" + (lotRepo.count() + 1));
            nw.setGrade(old.getGrade());
            nw.setWeight(old.getWeight());
            nw.setStorageFrom(today);
            nw.setStatus(Domain.LotStatus.IN_ZONE);
            nw.setSourceLot(old);
            nw = lotRepo.save(nw);
            to.setUsedKg(nz(to.getUsedKg()).add(nw.getWeight()));
            created.add(nw);
            moveDesc.add(String.format("%s(%s,%s级,%skg) → %s(%s)",
                    old.getLocationCode(), from.getName(), old.getGrade(), old.getWeight(),
                    nw.getLocationCode(), to.getName()));
        }
        if (created.isEmpty()) {
            throw new ApiException("货位已在目标温区，无需转仓");
        }
        trace.record(b, Domain.EventType.MOVE, "转仓：" + String.join("；", moveDesc)
                + "，原因=" + req.reason() + "；新库位仓储费自" + today + "起算", operator);
        return created;
    }

    @Transactional
    public Zone setZoneFault(Long zoneId, Dtos.ZoneFaultReq req) {
        Zone z = zoneRepo.findById(zoneId).orElseThrow(() -> new ApiException("温区不存在"));
        boolean wasFaulty = z.isFaulty();
        z.setFaulty(Boolean.TRUE.equals(req.faulty()));
        z.setFaultNote(req.note());
        if (z.isFaulty() && !wasFaulty) {
            z.setFaultSince(LocalDateTime.now());
            // 故障要写进每个在库批次的链条（同批次多等级库位汇总为一条）
            Map<Batch, List<StorageLot>> affected = new LinkedHashMap<>();
            for (StorageLot lot : lotRepo.findByStatusOrderByBatchArrivalAtAsc(Domain.LotStatus.IN_ZONE)) {
                if (lot.getZone().getId().equals(z.getId())) {
                    affected.computeIfAbsent(lot.getBatch(), k -> new ArrayList<>()).add(lot);
                }
            }
            for (Map.Entry<Batch, List<StorageLot>> e : affected.entrySet()) {
                String lotDesc = e.getValue().stream()
                        .map(l -> l.getGrade() + "级" + l.getWeight() + "kg").reduce((x, y) -> x + "、" + y).orElse("");
                trace.record(e.getKey(), Domain.EventType.ZONE_FAULT, String.format(
                        "温区故障：%s（%s）自%s起故障，受影响库存：%s，需转仓。%s",
                        z.getName(), z.getCode(), z.getFaultSince(), lotDesc,
                        req.note() == null ? "" : req.note()), "系统");
            }
        } else if (!z.isFaulty()) {
            z.setFaultSince(null);
        }
        return zoneRepo.save(z);
    }

    // ---------------- 改客户 / 催货 / 投诉 ----------------

    @Transactional
    public Batch changeCustomer(Dtos.CustomerChangeReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        Customer c = customerRepo.findById(req.customerId()).orElseThrow(() -> new ApiException("客户不存在"));
        String orig = b.getPlannedCustomer() == null ? "未指定" : b.getPlannedCustomer().getName();
        b.setPlannedCustomer(c);
        trace.record(b, Domain.EventType.CUSTOMER_CHANGE, String.format(
                "临时改客户：%s → %s。原因=%s", orig, c.getName(), req.reason()), operator);
        return b;
    }

    @Transactional
    public CustomerDemand demand(Dtos.DemandReq req, String operator) {
        Batch b = mustBatch(req.batchId());
        Customer c = customerRepo.findById(req.customerId()).orElseThrow(() -> new ApiException("客户不存在"));
        CustomerDemand d = new CustomerDemand();
        d.setBatch(b);
        d.setCustomer(c);
        d.setPriority(Boolean.TRUE.equals(req.priority()));
        d.setNote(req.note());
        d.setCreatedAt(LocalDateTime.now());
        d = demandRepo.save(d);
        trace.record(b, Domain.EventType.DEMAND, String.format(
                "客户催货：%s%s，需求说明=%s", c.getName(), d.isPriority() ? "（要求优先出库）" : "",
                req.note() == null ? "" : req.note()), operator);
        return d;
    }

    @Transactional
    public Complaint complaint(Dtos.ComplaintReq req, String operator) {
        Outbound o = outboundRepo.findById(req.outboundId()).orElseThrow(() -> new ApiException("出库单不存在"));
        Batch b = o.getBatch();
        BigDecimal price = req.unitPrice() == null ? o.getUnitPrice() : req.unitPrice();
        BigDecimal comp = price.multiply(req.badWeight()).setScale(2, RoundingMode.HALF_UP);
        Complaint c = new Complaint();
        c.setBatch(b);
        c.setOutbound(o);
        c.setContent(req.content());
        c.setBadWeight(req.badWeight());
        c.setUnitPrice(price);
        c.setCompensation(comp);
        c.setResponsible(req.responsible() == null ? Domain.Responsible.OWNER : req.responsible());
        c.setComplainedAt(LocalDateTime.now());
        c.setHandler(operator);
        complaintRepo.save(c);

        b.setComplaintDeductWeight(nz(b.getComplaintDeductWeight()).add(req.badWeight()));
        b.setComplaintDeductAmount(nz(b.getComplaintDeductAmount()).add(comp));
        trace.record(b, Domain.EventType.COMPLAINT, String.format(
                "出库后客户投诉坏果：客户=%s，出库%skg中坏果%skg，按%s元/kg赔付%s元，责任=%s，内容=%s",
                o.getCustomer().getName(), o.getWeight(), req.badWeight(), price, comp,
                c.getResponsible().label, req.content()), operator);
        return c;
    }

    // ---------------- 账单结算 ----------------

    @Transactional
    public Bill generateBill(Long batchId, String operator) {
        Batch b = mustBatch(batchId);
        billRepo.findByBatchIdAndStatus(batchId, Domain.BillStatus.DRAFT).ifPresent(billRepo::delete);
        billRepo.flush();

        Bill bill = new Bill();
        bill.setBillNo("B" + LocalDate.now().toString().replace("-", "") + "-" + String.format("%03d", billRepo.count() + 1));
        bill.setBatch(b);
        bill.setOwner(b.getOwner());
        bill.setSettleMethod(b.getSettleMethod());

        Category cat = b.getCategory();
        BigDecimal goods = BigDecimal.ZERO;
        List<Outbound> outs = outboundRepo.findByBatchIdOrderByOutboundAtAsc(b.getId());

        if (b.getSettleMethod() == Domain.SettleMethod.ORIGINAL) {
            // 货主要求按原重结算：以到货原重 × 质检初评定级参考价计价，再扣各项损耗
            Domain.Grade origGrade = qcRepo.findByBatchIdOrderByCheckedAtAsc(b.getId()).stream()
                    .map(QcRecord::getGrade).filter(Objects::nonNull).findFirst().orElse(Domain.Grade.B);
            BigDecimal ref = gradePrice(cat, origGrade);
            goods = b.getArrivalWeight().multiply(ref).setScale(2, RoundingMode.HALF_UP);
            addLine(bill, "GOODS", "按到货原重 " + b.getArrivalWeight() + "kg × " + origGrade + "级参考价 " + ref,
                    b.getArrivalWeight(), ref, goods);
            bill.setSettleWeight(b.getArrivalWeight());
            // 退货扣款
            if (nz(b.getReturnedWeight()).signum() > 0) {
                BigDecimal d = b.getReturnedWeight().multiply(ref).setScale(2, RoundingMode.HALF_UP);
                addLine(bill, "RETURN", "退货 " + b.getReturnedWeight() + "kg（原重结算下扣回）", b.getReturnedWeight(), ref, d.negate());
                bill.setReturnDeduction(d);
            }
            // 货主责任损耗扣款（分拣损耗 + 在仓报损）
            BigDecimal ownerLoss = sortRepo.findByBatchIdOrderBySortedAtAsc(b.getId()).stream()
                    .filter(s -> s.getLossResponsible() == Domain.Responsible.OWNER)
                    .map(SortRecord::getLossWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
            ownerLoss = ownerLoss.add(lossRepo.findByBatchIdOrderByLossAtAsc(b.getId()).stream()
                    .filter(l -> l.getResponsible() == Domain.Responsible.OWNER)
                    .map(LossRecord::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add));
            if (ownerLoss.signum() > 0) {
                BigDecimal d = ownerLoss.multiply(ref).setScale(2, RoundingMode.HALF_UP);
                addLine(bill, "LOSS", "货主责任损耗 " + ownerLoss + "kg（分拣争议/在仓报损）", ownerLoss, ref, d.negate());
                bill.setLossDeduction(d);
            }
            for (LossRecord lr : lossRepo.findByBatchIdOrderByLossAtAsc(b.getId())) {
                if (lr.getResponsible() != Domain.Responsible.OWNER) {
                    addLine(bill, "LOSS", "冷库/承运承担损耗 " + lr.getWeight() + "kg，不向货主扣款",
                            lr.getWeight(), null, BigDecimal.ZERO);
                    break;
                }
            }
        } else {
            // 按实际可售重量与实际出库等级/价格计价
            bill.setSettleWeight(nz(b.getShippedWeight()).add(openLotWeight(b.getId())));
            Map<Domain.Grade, BigDecimal[]> agg = new EnumMap<>(Domain.Grade.class);
            for (Outbound o : outs) {
                BigDecimal[] v = agg.computeIfAbsent(o.getGrade(), g -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                v[0] = v[0].add(o.getWeight());
                v[1] = v[1].add(o.getAmount());
            }
            for (Map.Entry<Domain.Grade, BigDecimal[]> e : agg.entrySet()) {
                goods = goods.add(e.getValue()[1]);
                addLine(bill, "GOODS", "实际出库 " + e.getKey() + "级 " + e.getValue()[0] + "kg（含锁价/实际成交价）",
                        e.getValue()[0], null, e.getValue()[1]);
            }
            // 未出库库存按参考价估值列示
            for (StorageLot lot : lotRepo.findByBatchIdOrderByIdAsc(b.getId())) {
                if (lot.getStatus() == Domain.LotStatus.IN_ZONE && lot.getWeight().signum() > 0) {
                    BigDecimal ref = gradePrice(cat, lot.getGrade());
                    BigDecimal v = lot.getWeight().multiply(ref).setScale(2, RoundingMode.HALF_UP);
                    goods = goods.add(v);
                    addLine(bill, "GOODS", "在库余货 " + lot.getGrade() + "级 " + lot.getWeight() + "kg（参考价估值）",
                            lot.getWeight(), ref, v);
                }
            }
            if (nz(b.getReturnedWeight()).signum() > 0) {
                addLine(bill, "RETURN", "退货 " + b.getReturnedWeight() + "kg（未计入可售货值）",
                        b.getReturnedWeight(), null, BigDecimal.ZERO);
            }
            BigDecimal totalLoss = nz(b.getLossWeight());
            if (totalLoss.signum() > 0) {
                addLine(bill, "LOSS", "累计损耗 " + totalLoss + "kg（实际结算下不产生货值）",
                        totalLoss, null, BigDecimal.ZERO);
            }
        }
        bill.setGoodsAmount(goods);

        // 仓储费：按库位段（含转仓前后）逐段 元/吨·天 计
        BigDecimal storageFee = BigDecimal.ZERO;
        for (StorageLot lot : lotRepo.findByBatchIdOrderByIdAsc(b.getId())) {
            LocalDate from = lot.getStorageFrom();
            long days;
            if (lot.getClosedAt() != null) {
                days = Math.max(1, ChronoUnit.DAYS.between(from, lot.getClosedAt()) + 1);
            } else {
                days = Math.max(1, ChronoUnit.DAYS.between(from, LocalDate.now()) + 1);
            }
            BigDecimal fee = lot.getWeight().divide(TON, 6, RoundingMode.HALF_UP)
                    .multiply(cat.getStorageFeePerTonDay())
                    .multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
            storageFee = storageFee.add(fee);
        }
        if (storageFee.signum() > 0) {
            addLine(bill, "STORAGE", "仓储费（按库位分段 " + cat.getStorageFeePerTonDay() + " 元/吨·天，含转仓前后）",
                    null, null, storageFee.negate());
            bill.setStorageFee(storageFee);
        }

        // 分拣费：按吨计 + 分拣工时
        BigDecimal sortFee = BigDecimal.ZERO;
        for (SortRecord s : sortRepo.findByBatchIdOrderBySortedAtAsc(b.getId())) {
            BigDecimal byWeight = s.getInputWeight().divide(TON, 6, RoundingMode.HALF_UP).multiply(cat.getSortFeePerTon());
            BigDecimal byLabor = s.getLaborHours().multiply(s.getLaborRate());
            sortFee = sortFee.add(byWeight).add(byLabor);
        }
        sortFee = sortFee.setScale(2, RoundingMode.HALF_UP);
        if (sortFee.signum() > 0) {
            addLine(bill, "SORT", "分拣费（按吨 " + cat.getSortFeePerTon() + " 元/吨 + 分拣工时）", null, null, sortFee.negate());
            bill.setSortFee(sortFee);
        }

        // 投诉赔付（货主责任才向货主扣）
        BigDecimal comp = complaintRepo.findByBatchIdOrderByComplainedAtAsc(b.getId()).stream()
                .filter(c -> c.getResponsible() == Domain.Responsible.OWNER)
                .map(Complaint::getCompensation).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (comp.signum() > 0) {
            addLine(bill, "COMPLAINT", "客户投诉坏果赔付（货主责任）", b.getComplaintDeductWeight(), null, comp.negate());
            bill.setComplaintDeduction(comp);
        }

        // 预付款抵扣（同一车货关联的农户预付款）
        BigDecimal advance = b.getOwner().getAdvanceBalance().min(
                goods.subtract(storageFee).subtract(sortFee).subtract(comp)
                        .subtract(bill.getLossDeduction()).subtract(bill.getReturnDeduction()));
        advance = advance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (advance.signum() > 0) {
            addLine(bill, "ADVANCE", "农户预付款抵扣（货主预付款余额 " + b.getOwner().getAdvanceBalance() + " 元）",
                    null, null, advance.negate());
            bill.setAdvanceDeduction(advance);
        }

        BigDecimal payable = goods.subtract(storageFee).subtract(sortFee).subtract(comp)
                .subtract(bill.getLossDeduction()).subtract(bill.getReturnDeduction())
                .subtract(advance).setScale(2, RoundingMode.HALF_UP);
        bill.setPayableAmount(payable);
        bill.setStatus(Domain.BillStatus.DRAFT);
        bill = billRepo.save(bill);

        trace.record(b, Domain.EventType.BILL, String.format(
                "生成账单%s：货值=%s，仓储费=%s，分拣费=%s，损耗扣款=%s，退货扣款=%s，投诉赔付=%s，预付款抵扣=%s，应付货主=%s（%s）",
                bill.getBillNo(), goods, storageFee, sortFee, bill.getLossDeduction(), bill.getReturnDeduction(),
                comp, advance, payable, b.getSettleMethod().label), operator);
        return bill;
    }

    @Transactional
    public Bill settleBill(Long billId, String operator) {
        Bill bill = billRepo.findById(billId).orElseThrow(() -> new ApiException("账单不存在"));
        if (bill.getStatus() != Domain.BillStatus.DRAFT) {
            throw new ApiException("账单已结算，不能重复结算");
        }
        Owner owner = bill.getOwner();
        if (bill.getAdvanceDeduction().signum() > 0) {
            owner.setAdvanceBalance(owner.getAdvanceBalance().subtract(bill.getAdvanceDeduction()));
        }
        bill.setStatus(Domain.BillStatus.SETTLED);
        bill.setSettleDate(LocalDate.now());
        Batch b = bill.getBatch();
        b.setStatus(Domain.BatchStatus.CLOSED);
        trace.record(b, Domain.EventType.SETTLE, String.format(
                "结算关账：账单%s，实付货主=%s元，结算日=%s", bill.getBillNo(), bill.getPayableAmount(), bill.getSettleDate()), operator);
        return bill;
    }

    private void addLine(Bill bill, String type, String summary, BigDecimal weight, BigDecimal price, BigDecimal amount) {
        BillLine l = new BillLine();
        l.setBill(bill);
        l.setLineType(type);
        l.setSummary(summary);
        l.setWeight(weight == null ? BigDecimal.ZERO : weight);
        l.setUnitPrice(price);
        l.setAmount(amount);
        bill.getLines().add(l);
    }

    // ---------------- 辅助 ----------------

    private void allocateLot(Batch b, Domain.Grade grade, BigDecimal weight, LocalDate from, StorageLot source) {
        if (weight.signum() == 0) return;
        List<Zone> zones = zoneRepo.findAllByOrderById().stream()
                .filter(z -> !z.isFaulty() && z.getZoneType().equals(b.getCategory().getTargetZoneType()))
                .sorted(Comparator.comparing(z -> z.getUsedKg().divide(z.getCapacityKg(), 4, RoundingMode.HALF_UP)))
                .toList();
        Zone target = zones.stream()
                .filter(z -> z.getCapacityKg().subtract(nz(z.getUsedKg())).compareTo(weight) >= 0)
                .findFirst()
                .orElseThrow(() -> new ApiException("没有容量充足且未故障的「" + b.getCategory().getTargetZoneType()
                        + "」温区可容纳 " + weight + "kg（可先转仓或扩容）"));
        StorageLot lot = new StorageLot();
        lot.setBatch(b);
        lot.setZone(target);
        lot.setLocationCode(target.getCode() + "-" + (lotRepo.count() + 1));
        lot.setGrade(grade);
        lot.setWeight(weight);
        lot.setStorageFrom(from);
        lot.setSourceLot(source);
        lotRepo.save(lot);
        target.setUsedKg(nz(target.getUsedKg()).add(weight));
    }

    private BigDecimal openLotWeight(Long batchId) {
        return lotRepo.findByBatchIdOrderByIdAsc(batchId).stream()
                .filter(l -> l.getStatus() == Domain.LotStatus.IN_ZONE)
                .map(StorageLot::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void refreshBatchStockStatus(Batch b) {
        BigDecimal open = openLotWeight(b.getId());
        if (open.signum() == 0) {
            if (nz(b.getShippedWeight()).signum() > 0 && b.getStatus() != Domain.BatchStatus.RETURNED) {
                b.setStatus(Domain.BatchStatus.SHIPPED);
            }
        } else if (nz(b.getShippedWeight()).signum() > 0) {
            b.setStatus(Domain.BatchStatus.PARTIAL_OUT);
        } else {
            b.setStatus(Domain.BatchStatus.IN_STORAGE);
        }
    }

    private BigDecimal gradePrice(Category c, Domain.Grade g) {
        return switch (g) {
            case A -> c.getPriceA();
            case B -> c.getPriceB();
            case C -> c.getPriceC();
        };
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int nzInt(Integer v) {
        return v == null ? 0 : v;
    }

    private Batch mustBatch(Long id) {
        return batchRepo.findById(id).orElseThrow(() -> new ApiException("批次不存在"));
    }
}
