package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsTaskOptionVO {

    private String  taskId;
    private String  taskName;
    private String  statYear;
    private Integer statQuarter;
    private Integer taskStatus;
    private String  taskStatusLabel;
}
