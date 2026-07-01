package com.kxhospital.cockpit.ds.qcb;

import lombok.Data;
import java.util.List;

@Data
public class DsQcbOrgDetailVO {
    private String  orgId;
    private String  orgName;
    private Integer cityCoveredCount;
    private Integer countyCoveredCount;
    private List<Integer> cityCenterIds;
    private List<Integer> countyCenterIds;
    private List<DsQcbCityOrgItemVO> cities;
}
