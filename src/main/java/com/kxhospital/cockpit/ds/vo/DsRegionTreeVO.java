package com.kxhospital.cockpit.ds.vo;

import lombok.Data;
import java.util.List;

@Data
public class DsRegionTreeVO {

    private String province = "浙江省";
    private List<CityNode> cities;

    @Data
    public static class CityNode {
        private Integer cityId;
        private Integer countyParentId;
        private String cityName;
        private List<CountyNode> counties;
    }

    @Data
    public static class CountyNode {
        private Integer countyId;
        private String countyName;
        private Integer countyParentId;
    }
}
