package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsCityDrillDownVO {

    private Integer cityId;
    private String  cityName;
    private Integer hasCityQc;
    private Integer submittedOrgCount;
    private java.util.List<DsCountyItemVO> counties;
    private java.util.List<DsOrgCoverageVO> orgs;
}
