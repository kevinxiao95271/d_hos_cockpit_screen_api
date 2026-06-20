package com.kxhospital.cockpit.ds.vo;

import lombok.Data;

@Data
public class DsStatSummaryVO {

    private String  taskId;
    private String  taskName;
    private String  statYear;
    private Integer statQuarter;
    private int meetingCount;
    private int meetingAttendeeTotal;
    private int trainingCount;
    private int trainingAttendeeTotal;
    private int surveyCount;
    private int guidanceCount;
    private int competitionCount;
    private int publicationCount;
}
