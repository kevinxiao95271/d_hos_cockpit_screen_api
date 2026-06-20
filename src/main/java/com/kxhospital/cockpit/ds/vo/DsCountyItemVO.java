package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsCountyItemVO {

    private Integer countyId;
    private String  countyName;
    private Integer hasCountyQc;
    private Integer coveredOrgCount;
    private Integer orgTotalCount;
}
