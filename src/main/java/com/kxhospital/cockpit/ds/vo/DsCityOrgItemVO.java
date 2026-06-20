package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsCityOrgItemVO {

    private Integer cityId;
    private Integer countyParentId;
    private String  cityName;
    private Integer hasCityQc;
    private Integer countyQcCount;
    private Integer countyTotal;
}
