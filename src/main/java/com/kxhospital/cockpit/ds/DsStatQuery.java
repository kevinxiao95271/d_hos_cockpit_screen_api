package com.kxhospital.cockpit.ds;

import com.kxhospital.cockpit.ds.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DsStatQuery {

    private final NamedParameterJdbcTemplate jdbc;

    @Cacheable(value = "dsStatSummary", key = "#taskId")
    public DsStatSummaryVO buildSummary(long taskId) {
        TaskMeta meta = loadTaskMeta(taskId);
        ModuleCounts counts = loadSingleTaskCounts(taskId);

        DsStatSummaryVO vo = new DsStatSummaryVO();
        vo.setTaskId(String.valueOf(taskId));
        vo.setTaskName(meta.taskName);
        vo.setStatYear(meta.statYear);
        vo.setStatQuarter(meta.statQuarter);
        vo.setMeetingCount(counts.meetingCount);
        vo.setMeetingAttendeeTotal(counts.meetingAttendeeTotal);
        vo.setTrainingCount(counts.trainingCount);
        vo.setTrainingAttendeeTotal(counts.trainingAttendeeTotal);
        vo.setSurveyCount(counts.surveyCount);
        vo.setGuidanceCount(counts.guidanceCount);
        vo.setCompetitionCount(counts.competitionCount);
        vo.setPublicationCount(counts.publicationCount);
        return vo;
    }

    @Cacheable("dsStatTrend")
    public DsStatTrendVO buildTrend() {
        String taskSql =
            "SELECT id, task_name, stat_year, stat_quarter " +
            "FROM wr_task " +
            "WHERE task_type = 'daily_work' AND status IN (1, 2) AND del_flag = 0 " +
            "ORDER BY stat_year ASC NULLS LAST, stat_quarter ASC NULLS LAST, id ASC";
        List<TaskMeta> tasks = jdbc.query(taskSql, Collections.<String, Object>emptyMap(), (rs, i) -> {
            TaskMeta t    = new TaskMeta();
            t.taskId      = rs.getLong("id");
            t.taskName    = rs.getString("task_name");
            t.statYear    = rs.getString("stat_year");
            int q         = rs.getInt("stat_quarter");
            t.statQuarter = rs.wasNull() ? null : q;
            return t;
        });
        if (tasks.isEmpty()) {
            DsStatTrendVO empty = new DsStatTrendVO();
            empty.setLabels(Collections.<String>emptyList());
            empty.setPeriods(Collections.<DsStatPeriodVO>emptyList());
            return empty;
        }

        Map<Long, ModuleCounts> countsMap = loadAllTaskCountsBulk();

        List<DsStatPeriodVO> periods = new ArrayList<>();
        for (TaskMeta task : tasks) {
            ModuleCounts counts = countsMap.getOrDefault(task.taskId, new ModuleCounts());
            DsStatPeriodVO p = new DsStatPeriodVO();
            p.setTaskId(String.valueOf(task.taskId));
            p.setLabel(buildLabel(task));
            p.setStatYear(task.statYear);
            p.setStatQuarter(task.statQuarter);
            p.setSubmittedOrgCount(counts.orgCount);
            p.setMeetingCount(counts.meetingCount);
            p.setMeetingAttendeeTotal(counts.meetingAttendeeTotal);
            p.setTrainingCount(counts.trainingCount);
            p.setTrainingAttendeeTotal(counts.trainingAttendeeTotal);
            p.setSurveyCount(counts.surveyCount);
            p.setGuidanceCount(counts.guidanceCount);
            p.setCompetitionCount(counts.competitionCount);
            p.setPublicationCount(counts.publicationCount);
            periods.add(p);
        }

        List<String> labels = periods.stream()
                .map(DsStatPeriodVO::getLabel)
                .collect(Collectors.toList());
        DsStatTrendVO trend = new DsStatTrendVO();
        trend.setLabels(labels);
        trend.setPeriods(periods);
        return trend;
    }

    private TaskMeta loadTaskMeta(long taskId) {
        String sql =
            "SELECT id, task_name, stat_year, stat_quarter " +
            "FROM wr_task WHERE id = :id AND del_flag = 0";
        List<TaskMeta> rows = jdbc.query(sql, p("id", taskId), (rs, i) -> {
            TaskMeta t    = new TaskMeta();
            t.taskId      = rs.getLong("id");
            t.taskName    = rs.getString("task_name");
            t.statYear    = rs.getString("stat_year");
            int q         = rs.getInt("stat_quarter");
            t.statQuarter = rs.wasNull() ? null : q;
            return t;
        });
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("DS: 任务不存在 taskId=" + taskId);
        }
        return rows.get(0);
    }

    private ModuleCounts loadSingleTaskCounts(long taskId) {
        Map<Long, ModuleCounts> map = loadTaskCountsBulk(
            "WHERE r.task_id = :taskId AND r.status IN (1,2) AND r.del_flag = 0",
            p("taskId", taskId)
        );
        return map.getOrDefault(taskId, new ModuleCounts());
    }

    private Map<Long, ModuleCounts> loadAllTaskCountsBulk() {
        return loadTaskCountsBulk(
            "WHERE r.status IN (1,2) AND r.del_flag = 0",
            Collections.<String, Object>emptyMap()
        );
    }

    private Map<Long, ModuleCounts> loadTaskCountsBulk(String whereClause, Map<String, Object> params) {
        String sql =
            "SELECT " +
            "  r.task_id, " +
            "  COUNT(DISTINCT r.id)               AS org_count, " +
            "  COALESCE(SUM(m_agg.cnt),  0)        AS meeting_cnt, " +
            "  COALESCE(SUM(m_agg.att),  0)        AS meeting_att, " +
            "  COALESCE(SUM(t_agg.cnt),  0)        AS training_cnt, " +
            "  COALESCE(SUM(t_agg.att),  0)        AS training_att, " +
            "  COALESCE(SUM(s_agg.cnt),  0)        AS survey_cnt, " +
            "  COALESCE(SUM(g_agg.cnt),  0)        AS guidance_cnt, " +
            "  COALESCE(SUM(bc_agg.cnt), 0)        AS competition_cnt, " +
            "  COALESCE(SUM(bp_agg.cnt), 0)        AS publication_cnt " +
            "FROM wr_record r " +
            "LEFT JOIN ( " +
            "  SELECT record_id, COUNT(*) AS cnt, COALESCE(SUM(attendee_count),0) AS att " +
            "  FROM dw_meeting WHERE del_flag=0 GROUP BY record_id " +
            ") m_agg  ON m_agg.record_id  = r.id " +
            "LEFT JOIN ( " +
            "  SELECT record_id, COUNT(*) AS cnt, COALESCE(SUM(attendee_count),0) AS att " +
            "  FROM dw_training WHERE del_flag=0 GROUP BY record_id " +
            ") t_agg  ON t_agg.record_id  = r.id " +
            "LEFT JOIN ( " +
            "  SELECT record_id, COUNT(*) AS cnt " +
            "  FROM dw_survey WHERE del_flag=0 GROUP BY record_id " +
            ") s_agg  ON s_agg.record_id  = r.id " +
            "LEFT JOIN ( " +
            "  SELECT record_id, COUNT(*) AS cnt " +
            "  FROM dw_guidance WHERE del_flag=0 GROUP BY record_id " +
            ") g_agg  ON g_agg.record_id  = r.id " +
            "LEFT JOIN ( " +
            "  SELECT record_id, COUNT(*) AS cnt " +
            "  FROM dw_bonus WHERE del_flag=0 AND bonus_type='competition' GROUP BY record_id " +
            ") bc_agg ON bc_agg.record_id = r.id " +
            "LEFT JOIN ( " +
            "  SELECT record_id, COUNT(*) AS cnt " +
            "  FROM dw_bonus WHERE del_flag=0 AND bonus_type='publication' GROUP BY record_id " +
            ") bp_agg ON bp_agg.record_id = r.id " +
            whereClause + " " +
            "GROUP BY r.task_id";

        Map<Long, ModuleCounts> result = new LinkedHashMap<>();
        jdbc.query(sql, params, rs -> {
            ModuleCounts c       = new ModuleCounts();
            long taskId          = rs.getLong("task_id");
            c.orgCount           = rs.getInt("org_count");
            c.meetingCount       = rs.getInt("meeting_cnt");
            c.meetingAttendeeTotal = rs.getInt("meeting_att");
            c.trainingCount      = rs.getInt("training_cnt");
            c.trainingAttendeeTotal = rs.getInt("training_att");
            c.surveyCount        = rs.getInt("survey_cnt");
            c.guidanceCount      = rs.getInt("guidance_cnt");
            c.competitionCount   = rs.getInt("competition_cnt");
            c.publicationCount   = rs.getInt("publication_cnt");
            result.put(taskId, c);
        });
        return result;
    }

    private String buildLabel(TaskMeta t) {
        if (t.statQuarter != null) {
            return (t.statYear != null ? t.statYear : "?") + "Q" + t.statQuarter;
        }
        return (t.statYear != null ? t.statYear : t.taskName) + "年度";
    }

    private static Map<String, Object> p(String key, Object value) {
        return Collections.singletonMap(key, value);
    }

    private static class TaskMeta {
        long    taskId;
        String  taskName;
        String  statYear;
        Integer statQuarter;
    }

    private static class ModuleCounts {
        int orgCount            = 0;
        int meetingCount        = 0;
        int meetingAttendeeTotal = 0;
        int trainingCount       = 0;
        int trainingAttendeeTotal = 0;
        int surveyCount         = 0;
        int guidanceCount       = 0;
        int competitionCount    = 0;
        int publicationCount    = 0;
    }
}
