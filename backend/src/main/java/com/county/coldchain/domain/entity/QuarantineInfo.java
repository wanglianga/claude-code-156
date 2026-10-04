package com.county.coldchain.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/** 检疫证明信息（票证）。 */
@Getter
@Setter
@Embeddable
public class QuarantineInfo {
    private String certificateNo;
    private String issuingOrg;
    /** 证明日期 yyyy-MM-dd 字符串，简化处理 */
    private String issueDate;
    /** 票证是否核验通过 */
    private Boolean verified;
}
