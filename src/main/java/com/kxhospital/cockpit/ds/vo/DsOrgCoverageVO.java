package com.kxhospital.cockpit.ds.vo;

import lombok.Data;
import java.util.LinkedHashMap;

@Data
public class DsOrgCoverageVO {

    private String  orgId;
    private String  orgName;
    private Integer submitStatus;
    private String  submitStatusLabel;
    private Integer hasCityQc;
    private LinkedHashMap<Integer, Integer> countyBits;
    private Integer coveredCountyCount;
    private Integer countyTotal;
}
