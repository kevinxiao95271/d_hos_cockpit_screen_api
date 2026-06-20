package com.kxhospital.cockpit.ds.vo;

import lombok.Data;
import java.util.List;

@Data
public class DsProvinceMapVO {

    private String taskId;
    private String taskName;
    private String statYear;
    private Integer statQuarter;
    private Integer submittedOrgCount;
    private Integer cityQcCount;
    private Integer cityTotal;
    private Integer countyQcCount;
    private Integer countyTotal;
    private List<DsCityItemVO> cities;
}
