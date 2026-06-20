package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsOrgSummaryVO {

    private String  orgId;
    private String  orgName;
    private Integer submitStatus;
    private String  submitStatusLabel;
    private Integer cityCoveredCount;
    private Integer countyCoveredCount;
}
