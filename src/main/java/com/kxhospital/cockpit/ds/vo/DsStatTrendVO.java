package com.kxhospital.cockpit.ds.vo;

import lombok.Data;
import java.util.List;

@Data
public class DsStatTrendVO {

    private List<String> labels;
    private List<DsStatPeriodVO> periods;
}
