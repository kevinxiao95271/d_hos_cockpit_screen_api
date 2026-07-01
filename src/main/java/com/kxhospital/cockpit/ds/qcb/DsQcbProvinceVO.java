package com.kxhospital.cockpit.ds.qcb;

import lombok.Data;
import java.util.List;

@Data
public class DsQcbProvinceVO {
    private Integer orgCount;
    private Integer cityQcCount;
    private Integer cityTotal;
    private Integer countyQcCount;
    private Integer countyTotal;
    private List<DsQcbCityItemVO> cities;
}
