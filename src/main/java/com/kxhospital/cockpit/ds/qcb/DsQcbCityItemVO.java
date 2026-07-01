package com.kxhospital.cockpit.ds.qcb;

import lombok.Data;

@Data
public class DsQcbCityItemVO {
    private Integer cityId;
    private Integer countyParentId;
    private String  cityName;
    private Integer hasCityQc;
    private Integer orgCoveredCount;
    private Integer orgTotalCount;
    private Integer countyQcCount;
    private Integer countyTotal;
    private Double  countyQcRate;
}
