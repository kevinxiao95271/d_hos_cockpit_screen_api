package com.kxhospital.cockpit.ds.qcb;

import com.kxhospital.cockpit.ds.vo.DsCountyItemVO;
import lombok.Data;
import java.util.List;

@Data
public class DsQcbCityDrillDownVO {
    private Integer cityId;
    private String  cityName;
    private Integer hasCityQc;
    private Integer orgCount;
    private List<DsCountyItemVO> counties;
    private List<DsQcbOrgCoverageVO> orgs;
}
