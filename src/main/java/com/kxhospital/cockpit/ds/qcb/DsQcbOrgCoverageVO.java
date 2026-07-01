package com.kxhospital.cockpit.ds.qcb;

import lombok.Data;
import java.util.LinkedHashMap;

@Data
public class DsQcbOrgCoverageVO {
    private String  orgId;
    private String  orgName;
    private Integer hasCityQc;
    private LinkedHashMap<Integer, Integer> countyBits;
    private Integer coveredCountyCount;
    private Integer countyTotal;
}
