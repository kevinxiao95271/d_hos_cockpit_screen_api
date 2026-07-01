package com.kxhospital.cockpit.ds.qcb;

import lombok.Data;

@Data
public class DsQcbCityOrgItemVO {
    private Integer cityId;
    private Integer countyParentId;
    private String  cityName;
    private Integer hasCityQc;
    private Integer countyQcCount;
    private Integer countyTotal;
}
