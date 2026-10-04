package com.county.coldchain.domain;

/**
 * 领域枚举集中定义。
 */
public final class Domain {

    private Domain() {
    }

    /** 货主类型 */
    public enum OwnerType { FARMER("农户"), COOP("合作社"), WHOLESALER("批发商");
        public final String label;
        OwnerType(String label) { this.label = label; }
    }

    /** 结算方式：按实际可售重量 / 货主要求按原重结算 */
    public enum SettleMethod { ACTUAL("按实际可售重量"), ORIGINAL("按到货原重");
        public final String label;
        SettleMethod(String label) { this.label = label; }
    }

    /** 批次状态 */
    public enum BatchStatus {
        REGISTERED("待质检"),
        SORTING("待分拣"),
        IN_STORAGE("在仓"),
        PARTIAL_OUT("部分出库"),
        RETURNED("已退货"),
        SHIPPED("已出清"),
        CLOSED("已结算关账");
        public final String label;
        BatchStatus(String label) { this.label = label; }
    }

    /** 质检结论：入库 / 降级 / 退货 / 分拣 */
    public enum QcResult { INBOUND("入库"), DEGRADE("降级入库"), RETURN("退货"), SORT("转分拣");
        public final String label;
        QcResult(String label) { this.label = label; }
    }

    public enum Grade { A("A级"), B("B级"), C("C级");
        public final String label;
        Grade(String label) { this.label = label; }
    }

    /** 库存库位状态 */
    public enum LotStatus { PENDING_SORT("待分拣"), IN_ZONE("在库"), TRANSFERRED("已转仓"), OUT("已出库"), SCRAPPED("已报损");
        public final String label;
        LotStatus(String label) { this.label = label; }
    }

    /** 出库渠道：正常批发 / 加工渠道 */
    public enum Channel { NORMAL("批发客户"), PROCESSING("加工渠道");
        public final String label;
        Channel(String label) { this.label = label; }
    }

    /** 责任方：货主 / 冷库 / 承运 */
    public enum Responsible { OWNER("货主"), COLD_STORAGE("冷库"), CARRIER("承运方");
        public final String label;
        Responsible(String label) { this.label = label; }
    }

    public enum DemandStatus { OPEN("催货中"), FULFILLED("已满足");
        public final String label;
        DemandStatus(String label) { this.label = label; }
    }

    public enum BillStatus { DRAFT("待结算"), SETTLED("已结算");
        public final String label;
        BillStatus(String label) { this.label = label; }
    }

    /** 批次链事件类型 */
    public enum EventType {
        REGISTER("到货登记"),
        QC("质检验收"),
        SORT("分拣"),
        MOVE("转仓"),
        LOSS("报损"),
        OUTBOUND("出库"),
        COMPLAINT("客户投诉"),
        DEMAND("客户催货"),
        ZONE_FAULT("温区故障"),
        CUSTOMER_CHANGE("临时改客户"),
        RETURN("退货"),
        BILL("账单"),
        SETTLE("结算关账");
        public final String label;
        EventType(String label) { this.label = label; }
    }
}
