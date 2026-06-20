package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsStatPeriodVO {

    private String  taskId;
    private String  label;
    private String  statYear;
    private Integer statQuarter;
    private int submittedOrgCount;
    private int meetingCount;
    private int meetingAttendeeTotal;
    private int trainingCount;
    private int trainingAttendeeTotal;
    private int surveyCount;
    private int guidanceCount;
    private int competitionCount;
    private int publicationCount;
}
