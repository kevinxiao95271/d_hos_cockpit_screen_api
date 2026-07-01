package com.kxhospital.cockpit.ds.qcb;

import lombok.Data;

@Data
public class DsQcbOrgSummaryVO {
    private String  orgId;
    private String  orgName;
    private Integer cityCoveredCount;
    private Integer countyCoveredCount;
}
