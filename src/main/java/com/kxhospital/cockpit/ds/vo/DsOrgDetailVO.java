package com.kxhospital.cockpit.ds.vo;

import lombok.Data;
import java.util.List;

@Data
public class DsOrgDetailVO {

    private String  orgId;
    private String  orgName;
    private Integer submitStatus;
    private String  submitStatusLabel;
    private Integer cityCoveredCount;
    private Integer countyCoveredCount;
    private List<Integer> cityCenterIds;
    private List<Integer> countyCenterIds;
    private List<DsCityOrgItemVO> cities;
}
