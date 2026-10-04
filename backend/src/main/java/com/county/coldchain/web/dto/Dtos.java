package com.county.coldchain.web.dto;

import com.county.coldchain.domain.Domain;

import java.math.BigDecimal;

/** 接口请求体集中定义 */
public final class Dtos {
    private Dtos() {}

    public record LoginReq(String username, String password) {}

    public record RegisterReq(
            String vehicleNo, Long ownerId, Long categoryId, String origin,
            BigDecimal arrivalWeight, BigDecimal arrivalTemp, String packaging,
            String quarantineNo, Long plannedCustomerId, Domain.SettleMethod settleMethod,
            Boolean mixedLoad) {}

    public record QcReq(
            Long batchId, Integer appearanceScore, Integer maturityScore,
            BigDecimal rotRate, BigDecimal moistureRate, BigDecimal measuredTemp,
            Boolean documentsOk, Domain.Grade grade, Domain.QcResult result,
            BigDecimal acceptedWeight, String remark) {}

    public record SortReq(
            Long batchId, BigDecimal inputWeight,
            BigDecimal weightA, BigDecimal weightB, BigDecimal weightC,
            BigDecimal laborHours, BigDecimal laborRate,
            Domain.Responsible lossResponsible, Boolean disputed, String disputeNote,
            String remark) {}

    public record LossReq(Long batchId, Long lotId, Domain.Grade grade, BigDecimal weight,
                          String reason, Domain.Responsible responsible) {}

    public record OutboundReq(Long batchId, Long customerId, Domain.Grade grade,
                              BigDecimal weight, Domain.Channel channel,
                              Boolean priority, Boolean customerChanged,
                              Long priceLockId, String remark) {}

    public record TransferReq(Long batchId, Long toZoneId, String reason) {}

    public record ZoneFaultReq(Boolean faulty, String note) {}

    public record CustomerChangeReq(Long batchId, Long customerId, String reason) {}

    public record DemandReq(Long batchId, Long customerId, Boolean priority, String note) {}

    public record ComplaintReq(Long outboundId, String content, BigDecimal badWeight,
                               BigDecimal unitPrice, Domain.Responsible responsible) {}

    public record OwnerReq(String code, String name, Domain.OwnerType type, String phone) {}

    public record CustomerReq(String name, String phone, String contact) {}

    public record ZoneReq(String code, String name, String zoneType,
                          BigDecimal targetTemp, BigDecimal capacityKg) {}

    public record CategoryReq(String name, BigDecimal maxArrivalTemp, String targetZoneType,
                              BigDecimal priceA, BigDecimal priceB, BigDecimal priceC,
                              BigDecimal storageFeePerTonDay, BigDecimal sortFeePerTon,
                              Integer shelfLifeDays) {}

    public record PriceLockReq(Long customerId, Long categoryId, Domain.Grade grade,
                               BigDecimal lockedPrice, String validFrom, String validTo,
                               BigDecimal lockedQtyKg) {}

    public record AdvanceReq(BigDecimal amount) {}
}
