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
public class DsQcQuery {

    private final NamedParameterJdbcTemplate jdbc;

    @Cacheable("dsRegions")
    public DsRegionTreeVO buildRegionTree() {
        List<RegionRow> cityRegions = loadCityRegions();
        Map<Integer, List<RegionRow>> countyByParent = loadCountiesByParentAll();
        Map<Integer, String> parentNames = loadCountyParentNames();

        List<DsRegionTreeVO.CityNode> cityNodes = new ArrayList<>();
        for (RegionRow city : cityRegions) {
            int countyParentId = city.id + 100;
            String cityName = parentNames.getOrDefault(countyParentId, city.name.replace("市级", "市"));
            List<RegionRow> counties = countyByParent.getOrDefault(countyParentId, Collections.<RegionRow>emptyList());

            List<DsRegionTreeVO.CountyNode> countyNodes = new ArrayList<>();
            for (RegionRow county : counties) {
                DsRegionTreeVO.CountyNode cn = new DsRegionTreeVO.CountyNode();
                cn.setCountyId(county.id);
                cn.setCountyName(county.name);
                cn.setCountyParentId(countyParentId);
                countyNodes.add(cn);
            }

            DsRegionTreeVO.CityNode cityNode = new DsRegionTreeVO.CityNode();
            cityNode.setCityId(city.id);
            cityNode.setCountyParentId(countyParentId);
            cityNode.setCityName(cityName);
            cityNode.setCounties(countyNodes);
            cityNodes.add(cityNode);
        }

        DsRegionTreeVO tree = new DsRegionTreeVO();
        tree.setCities(cityNodes);
        return tree;
    }

    @Cacheable(value = "dsOrgs", key = "#taskId")
    public List<DsOrgSummaryVO> listOrgSummaries(long taskId) {
        List<OrgRow> orgs = loadOrgs(taskId, Collections.<Long>emptyList());
        return orgs.stream().map(o -> {
            DsOrgSummaryVO vo = new DsOrgSummaryVO();
            vo.setOrgId(String.valueOf(o.orgId));
            vo.setOrgName(o.orgName);
            vo.setSubmitStatus(o.status);
            vo.setSubmitStatusLabel(o.status == 2 ? "已通过" : "已提交");
            vo.setCityCoveredCount(o.cityCenterIds.size());
            vo.setCountyCoveredCount(o.countyCenterIds.size());
            return vo;
        }).collect(Collectors.toList());
    }

    @Cacheable("dsTasks")
    public List<DsTaskOptionVO> listTasks() {
        String sql =
            "SELECT id, task_name, stat_year, stat_quarter, status " +
            "FROM wr_task " +
            "WHERE task_type = 'daily_work' " +
            "  AND status IN (1, 2) " +
            "  AND del_flag = 0 " +
            "ORDER BY stat_year DESC NULLS LAST, " +
            "         stat_quarter DESC NULLS FIRST, " +
            "         id DESC";
        return jdbc.query(sql, Collections.<String, Object>emptyMap(), (rs, i) -> {
            DsTaskOptionVO vo = new DsTaskOptionVO();
            vo.setTaskId(String.valueOf(rs.getLong("id")));
            vo.setTaskName(rs.getString("task_name"));
            vo.setStatYear(rs.getString("stat_year"));
            int q = rs.getInt("stat_quarter");
            vo.setStatQuarter(rs.wasNull() ? null : q);
            int s = rs.getInt("status");
            vo.setTaskStatus(s);
            vo.setTaskStatusLabel(s == 1 ? "进行中" : "已结束");
            return vo;
        });
    }

    @Cacheable(value = "dsProvince", key = "#taskId + '_' + #orgIdFilter")
    public DsProvinceMapVO buildProvinceMap(long taskId, List<Long> orgIdFilter) {
        TaskRow task                          = loadTask(taskId);
        List<OrgRow> orgs                     = loadOrgs(taskId, orgIdFilter);
        List<RegionRow> cityRegions           = loadCityRegions();
        Map<Integer, List<RegionRow>> byParent = loadCountiesByParentAll();
        Map<Integer, String> parentNames      = loadCountyParentNames();

        Set<Integer> allCityIds   = new HashSet<>();
        Set<Integer> allCountyIds = new HashSet<>();
        for (OrgRow org : orgs) {
            allCityIds.addAll(org.cityCenterIds);
            allCountyIds.addAll(org.countyCenterIds);
        }

        List<DsCityItemVO> cities = new ArrayList<>();
        int totalCountyQc = 0;
        int totalCounty   = 0;

        for (RegionRow city : cityRegions) {
            int countyParentId            = city.id + 100;
            List<RegionRow> counties      = byParent.getOrDefault(countyParentId, Collections.<RegionRow>emptyList());
            int countyTotal               = counties.size();
            int countyQcCount             = (int) counties.stream().filter(c -> allCountyIds.contains(c.id)).count();
            int orgCoveredCount           = (int) orgs.stream().filter(o -> o.cityCenterIds.contains(city.id)).count();
            String cityName               = parentNames.getOrDefault(countyParentId, city.name.replace("市级", "市"));
            double rate                   = countyTotal > 0
                    ? Math.round((double) countyQcCount / countyTotal * 10000.0) / 10000.0 : 0.0;

            DsCityItemVO vo = new DsCityItemVO();
            vo.setCityId(city.id);
            vo.setCountyParentId(countyParentId);
            vo.setCityName(cityName);
            vo.setHasCityQc(allCityIds.contains(city.id) ? 1 : 0);
            vo.setOrgCoveredCount(orgCoveredCount);
            vo.setOrgTotalCount(orgs.size());
            vo.setCountyQcCount(countyQcCount);
            vo.setCountyTotal(countyTotal);
            vo.setCountyQcRate(rate);
            cities.add(vo);

            totalCountyQc += countyQcCount;
            totalCounty   += countyTotal;
        }

        DsProvinceMapVO result = new DsProvinceMapVO();
        result.setTaskId(String.valueOf(taskId));
        result.setTaskName(task.taskName);
        result.setStatYear(task.statYear);
        result.setStatQuarter(task.statQuarter);
        result.setSubmittedOrgCount(orgs.size());
        result.setCityQcCount((int) cities.stream().filter(c -> c.getHasCityQc() == 1).count());
        result.setCityTotal(cities.size());
        result.setCountyQcCount(totalCountyQc);
        result.setCountyTotal(totalCounty);
        result.setCities(cities);
        return result;
    }

    @Cacheable(value = "dsCity", key = "#taskId + '_' + #cityId + '_' + #orgIdFilter")
    public DsCityDrillDownVO buildCityDrillDown(long taskId, int cityId, List<Long> orgIdFilter) {
        List<OrgRow> orgs          = loadOrgs(taskId, orgIdFilter);
        int countyParentId         = cityId + 100;
        List<RegionRow> counties   = loadCountiesByParent(countyParentId);
        String cityName            = loadCountyParentName(countyParentId);
        boolean anyCityQc          = orgs.stream().anyMatch(o -> o.cityCenterIds.contains(cityId));

        List<DsCountyItemVO> countyVOs = counties.stream().map(c -> {
            int covered = (int) orgs.stream().filter(o -> o.countyCenterIds.contains(c.id)).count();
            DsCountyItemVO vo = new DsCountyItemVO();
            vo.setCountyId(c.id);
            vo.setCountyName(c.name);
            vo.setHasCountyQc(covered > 0 ? 1 : 0);
            vo.setCoveredOrgCount(covered);
            vo.setOrgTotalCount(orgs.size());
            return vo;
        }).collect(Collectors.toList());

        List<DsOrgCoverageVO> orgVOs = orgs.stream().map(o -> {
            LinkedHashMap<Integer, Integer> bits = new LinkedHashMap<>();
            int coveredCount = 0;
            for (RegionRow county : counties) {
                int bit = o.countyCenterIds.contains(county.id) ? 1 : 0;
                bits.put(county.id, bit);
                coveredCount += bit;
            }
            DsOrgCoverageVO vo = new DsOrgCoverageVO();
            vo.setOrgId(String.valueOf(o.orgId));
            vo.setOrgName(o.orgName);
            vo.setSubmitStatus(o.status);
            vo.setSubmitStatusLabel(o.status == 2 ? "已通过" : "已提交");
            vo.setHasCityQc(o.cityCenterIds.contains(cityId) ? 1 : 0);
            vo.setCountyBits(bits);
            vo.setCoveredCountyCount(coveredCount);
            vo.setCountyTotal(counties.size());
            return vo;
        }).collect(Collectors.toList());

        DsCityDrillDownVO result = new DsCityDrillDownVO();
        result.setCityId(cityId);
        result.setCityName(cityName);
        result.setHasCityQc(anyCityQc ? 1 : 0);
        result.setSubmittedOrgCount(orgs.size());
        result.setCounties(countyVOs);
        result.setOrgs(orgVOs);
        return result;
    }

    public DsOrgDetailVO buildOrgDetail(long taskId, long orgId) {
        OrgRow org = loadSingleOrg(taskId, orgId);
        if (org == null) {
            return null;
        }

        List<RegionRow> cityRegions            = loadCityRegions();
        Map<Integer, List<RegionRow>> byParent = loadCountiesByParentAll();
        Map<Integer, String> parentNames       = loadCountyParentNames();

        List<DsCityOrgItemVO> cityItems = cityRegions.stream().map(city -> {
            int countyParentId          = city.id + 100;
            List<RegionRow> counties    = byParent.getOrDefault(countyParentId, Collections.<RegionRow>emptyList());
            int coveredCounty           = (int) counties.stream().filter(c -> org.countyCenterIds.contains(c.id)).count();
            String cityName             = parentNames.getOrDefault(countyParentId, city.name.replace("市级", "市"));

            DsCityOrgItemVO vo = new DsCityOrgItemVO();
            vo.setCityId(city.id);
            vo.setCountyParentId(countyParentId);
            vo.setCityName(cityName);
            vo.setHasCityQc(org.cityCenterIds.contains(city.id) ? 1 : 0);
            vo.setCountyQcCount(coveredCounty);
            vo.setCountyTotal(counties.size());
            return vo;
        }).collect(Collectors.toList());

        DsOrgDetailVO result = new DsOrgDetailVO();
        result.setOrgId(String.valueOf(org.orgId));
        result.setOrgName(org.orgName);
        result.setSubmitStatus(org.status);
        result.setSubmitStatusLabel(org.status == 2 ? "已通过" : "已提交");
        result.setCityCoveredCount(org.cityCenterIds.size());
        result.setCountyCoveredCount(org.countyCenterIds.size());
        result.setCityCenterIds(new ArrayList<>(org.cityCenterIds));
        result.setCountyCenterIds(new ArrayList<>(org.countyCenterIds));
        result.setCities(cityItems);
        return result;
    }

    private TaskRow loadTask(long taskId) {
        String sql =
            "SELECT id, task_name, stat_year, stat_quarter " +
            "FROM wr_task " +
            "WHERE id = :id AND del_flag = 0";
        List<TaskRow> rows = jdbc.query(sql, p("id", taskId), (rs, i) -> {
            TaskRow r    = new TaskRow();
            r.id         = rs.getLong("id");
            r.taskName   = rs.getString("task_name");
            r.statYear   = rs.getString("stat_year");
            int q        = rs.getInt("stat_quarter");
            r.statQuarter = rs.wasNull() ? null : q;
            return r;
        });
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("DS: 任务不存在 taskId=" + taskId);
        }
        return rows.get(0);
    }

    private List<OrgRow> loadOrgs(long taskId, List<Long> orgIdFilter) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.org_id, r.org_name, r.status, " +
            "       COALESCE(nb.city_center_ids,   '[]') AS city_ids, " +
            "       COALESCE(nb.county_center_ids, '[]') AS county_ids " +
            "FROM wr_record r " +
            "LEFT JOIN dw_network_build nb " +
            "       ON nb.record_id = r.id AND nb.del_flag = 0 " +
            "WHERE r.task_id   = :taskId " +
            "  AND r.status   IN (1, 2) " +
            "  AND r.del_flag  = 0"
        );
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);

        if (orgIdFilter != null && !orgIdFilter.isEmpty()) {
            sql.append(" AND r.org_id IN (:orgIds)");
            params.put("orgIds", orgIdFilter);
        }
        sql.append(" ORDER BY r.org_name");

        return jdbc.query(sql.toString(), params, (rs, i) -> {
            OrgRow o           = new OrgRow();
            o.orgId            = rs.getLong("org_id");
            o.orgName          = rs.getString("org_name");
            o.status           = rs.getInt("status");
            o.cityCenterIds    = parseIds(rs.getString("city_ids"));
            o.countyCenterIds  = parseIds(rs.getString("county_ids"));
            return o;
        });
    }

    private OrgRow loadSingleOrg(long taskId, long orgId) {
        String sql =
            "SELECT r.org_id, r.org_name, r.status, " +
            "       COALESCE(nb.city_center_ids,   '[]') AS city_ids, " +
            "       COALESCE(nb.county_center_ids, '[]') AS county_ids " +
            "FROM wr_record r " +
            "LEFT JOIN dw_network_build nb " +
            "       ON nb.record_id = r.id AND nb.del_flag = 0 " +
            "WHERE r.task_id   = :taskId " +
            "  AND r.org_id    = :orgId " +
            "  AND r.status   IN (1, 2) " +
            "  AND r.del_flag  = 0";
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("orgId",  orgId);
        List<OrgRow> rows = jdbc.query(sql, params, (rs, i) -> {
            OrgRow o          = new OrgRow();
            o.orgId           = rs.getLong("org_id");
            o.orgName         = rs.getString("org_name");
            o.status          = rs.getInt("status");
            o.cityCenterIds   = parseIds(rs.getString("city_ids"));
            o.countyCenterIds = parseIds(rs.getString("county_ids"));
            return o;
        });
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<RegionRow> loadCityRegions() {
        String sql =
            "SELECT id, name FROM dw_region " +
            "WHERE tree_type = 'city' " +
            "ORDER BY sort_order";
        return jdbc.query(sql, Collections.<String, Object>emptyMap(), (rs, i) -> {
            RegionRow r = new RegionRow();
            r.id   = rs.getInt("id");
            r.name = rs.getString("name");
            return r;
        });
    }

    private Map<Integer, List<RegionRow>> loadCountiesByParentAll() {
        String sql =
            "SELECT id, parent_id, name FROM dw_region " +
            "WHERE tree_type = 'county' AND level = 3 " +
            "ORDER BY parent_id, sort_order";
        List<RegionRow> rows = jdbc.query(sql, Collections.<String, Object>emptyMap(), (rs, i) -> {
            RegionRow r  = new RegionRow();
            r.id         = rs.getInt("id");
            r.parentId   = rs.getInt("parent_id");
            r.name       = rs.getString("name");
            return r;
        });
        Map<Integer, List<RegionRow>> map = new LinkedHashMap<>();
        for (RegionRow row : rows) {
            map.computeIfAbsent(row.parentId, k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private List<RegionRow> loadCountiesByParent(int parentId) {
        String sql =
            "SELECT id, name FROM dw_region " +
            "WHERE tree_type = 'county' AND parent_id = :pid AND level = 3 " +
            "ORDER BY sort_order";
        return jdbc.query(sql, p("pid", parentId), (rs, i) -> {
            RegionRow r = new RegionRow();
            r.id   = rs.getInt("id");
            r.name = rs.getString("name");
            return r;
        });
    }

    private Map<Integer, String> loadCountyParentNames() {
        String sql =
            "SELECT id, name FROM dw_region " +
            "WHERE tree_type = 'county' AND level = 2 " +
            "ORDER BY sort_order";
        Map<Integer, String> map = new LinkedHashMap<>();
        jdbc.query(sql, Collections.<String, Object>emptyMap(), rs -> {
            map.put(rs.getInt("id"), rs.getString("name"));
        });
        return map;
    }

    private String loadCountyParentName(int countyParentId) {
        String sql =
            "SELECT name FROM dw_region " +
            "WHERE id = :id AND tree_type = 'county' AND level = 2";
        List<String> rows = jdbc.query(sql, p("id", countyParentId), (rs, i) -> rs.getString("name"));
        return rows.isEmpty() ? "未知城市" : rows.get(0);
    }

    private Set<Integer> parseIds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String s = json.trim().replaceAll("[\\[\\]\\s]", "");
        if (s.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (String part : s.split(",")) {
            part = part.trim();
            if (!part.isEmpty()) {
                try {
                    result.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private static Map<String, Object> p(String key, Object value) {
        return Collections.singletonMap(key, value);
    }

    private static class TaskRow {
        long    id;
        String  taskName;
        String  statYear;
        Integer statQuarter;
    }

    private static class OrgRow {
        long         orgId;
        String       orgName;
        int          status;
        Set<Integer> cityCenterIds   = Collections.emptySet();
        Set<Integer> countyCenterIds = Collections.emptySet();
    }

    private static class RegionRow {
        int    id;
        int    parentId;
        String name;
    }
}
