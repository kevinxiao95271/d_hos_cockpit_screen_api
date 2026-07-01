package com.kxhospital.cockpit.ds.qcb;

import com.kxhospital.cockpit.ds.vo.DsCountyItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DsQcbQuery {

    private final NamedParameterJdbcTemplate jdbc;

    @Cacheable("dsQcbProvince")
    public DsQcbProvinceVO buildProvince() {
        List<OrgRow> orgs                     = loadOrgRows();
        List<RegionRow> cityRegions           = loadCityRegions();
        Map<Integer, List<RegionRow>> byParent = loadCountiesByParentAll();
        Map<Integer, String> parentNames      = loadCountyParentNames();

        Set<Integer> allCityIds   = new HashSet<>();
        Set<Integer> allCountyIds = new HashSet<>();
        for (OrgRow org : orgs) {
            allCityIds.addAll(org.cityCenterIds);
            allCountyIds.addAll(org.countyCenterIds);
        }

        List<DsQcbCityItemVO> cities = new ArrayList<>();
        int totalCountyQc = 0, totalCounty = 0;

        for (RegionRow city : cityRegions) {
            int countyParentId          = city.id + 100;
            List<RegionRow> counties    = byParent.getOrDefault(countyParentId, Collections.emptyList());
            int countyTotal             = counties.size();
            int countyQcCount           = (int) counties.stream().filter(c -> allCountyIds.contains(c.id)).count();
            int orgCoveredCount         = (int) orgs.stream().filter(o -> o.cityCenterIds.contains(city.id)).count();
            String cityName             = parentNames.getOrDefault(countyParentId, city.name.replace("市级", "市"));
            double rate                 = countyTotal > 0
                    ? Math.round((double) countyQcCount / countyTotal * 10000.0) / 10000.0 : 0.0;

            DsQcbCityItemVO vo = new DsQcbCityItemVO();
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

        DsQcbProvinceVO result = new DsQcbProvinceVO();
        result.setOrgCount(orgs.size());
        result.setCityQcCount((int) cities.stream().filter(c -> c.getHasCityQc() == 1).count());
        result.setCityTotal(cities.size());
        result.setCountyQcCount(totalCountyQc);
        result.setCountyTotal(totalCounty);
        result.setCities(cities);
        return result;
    }

    @Cacheable(value = "dsQcbCity", key = "#cityId")
    public DsQcbCityDrillDownVO buildCityDrillDown(int cityId) {
        List<OrgRow> orgs        = loadOrgRows();
        int countyParentId       = cityId + 100;
        List<RegionRow> counties = loadCountiesByParent(countyParentId);
        String cityName          = loadCountyParentName(countyParentId);
        boolean anyCityQc        = orgs.stream().anyMatch(o -> o.cityCenterIds.contains(cityId));

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

        List<DsQcbOrgCoverageVO> orgVOs = orgs.stream().map(o -> {
            LinkedHashMap<Integer, Integer> bits = new LinkedHashMap<>();
            int coveredCount = 0;
            for (RegionRow county : counties) {
                int bit = o.countyCenterIds.contains(county.id) ? 1 : 0;
                bits.put(county.id, bit);
                coveredCount += bit;
            }
            DsQcbOrgCoverageVO vo = new DsQcbOrgCoverageVO();
            vo.setOrgId(String.valueOf(o.orgId));
            vo.setOrgName(o.orgName);
            vo.setHasCityQc(o.cityCenterIds.contains(cityId) ? 1 : 0);
            vo.setCountyBits(bits);
            vo.setCoveredCountyCount(coveredCount);
            vo.setCountyTotal(counties.size());
            return vo;
        }).collect(Collectors.toList());

        DsQcbCityDrillDownVO result = new DsQcbCityDrillDownVO();
        result.setCityId(cityId);
        result.setCityName(cityName);
        result.setHasCityQc(anyCityQc ? 1 : 0);
        result.setOrgCount(orgs.size());
        result.setCounties(countyVOs);
        result.setOrgs(orgVOs);
        return result;
    }

    public DsQcbOrgDetailVO buildOrgDetail(long orgId) {
        OrgRow org = loadSingleOrg(orgId);
        if (org == null) return null;

        List<RegionRow> cityRegions            = loadCityRegions();
        Map<Integer, List<RegionRow>> byParent = loadCountiesByParentAll();
        Map<Integer, String> parentNames       = loadCountyParentNames();

        List<DsQcbCityOrgItemVO> cityItems = cityRegions.stream().map(city -> {
            int countyParentId       = city.id + 100;
            List<RegionRow> counties = byParent.getOrDefault(countyParentId, Collections.emptyList());
            int coveredCounty        = (int) counties.stream().filter(c -> org.countyCenterIds.contains(c.id)).count();
            String cityName          = parentNames.getOrDefault(countyParentId, city.name.replace("市级", "市"));

            DsQcbCityOrgItemVO vo = new DsQcbCityOrgItemVO();
            vo.setCityId(city.id);
            vo.setCountyParentId(countyParentId);
            vo.setCityName(cityName);
            vo.setHasCityQc(org.cityCenterIds.contains(city.id) ? 1 : 0);
            vo.setCountyQcCount(coveredCounty);
            vo.setCountyTotal(counties.size());
            return vo;
        }).collect(Collectors.toList());

        DsQcbOrgDetailVO result = new DsQcbOrgDetailVO();
        result.setOrgId(String.valueOf(org.orgId));
        result.setOrgName(org.orgName);
        result.setCityCoveredCount(org.cityCenterIds.size());
        result.setCountyCoveredCount(org.countyCenterIds.size());
        result.setCityCenterIds(new ArrayList<>(org.cityCenterIds));
        result.setCountyCenterIds(new ArrayList<>(org.countyCenterIds));
        result.setCities(cityItems);
        return result;
    }

    @Cacheable("dsQcbOrgs")
    public List<DsQcbOrgSummaryVO> listOrgSummaries() {
        return loadOrgRows().stream().map(o -> {
            DsQcbOrgSummaryVO vo = new DsQcbOrgSummaryVO();
            vo.setOrgId(String.valueOf(o.orgId));
            vo.setOrgName(o.orgName);
            vo.setCityCoveredCount(o.cityCenterIds.size());
            vo.setCountyCoveredCount(o.countyCenterIds.size());
            return vo;
        }).collect(Collectors.toList());
    }

    // ────────────── private helpers ──────────────

    private List<OrgRow> loadOrgRows() {
        String sql =
            "SELECT nb.org_id, nb.city_center_ids, nb.county_center_ids, " +
            "       r.org_name " +
            "FROM qc_network_build nb " +
            "LEFT JOIN (SELECT DISTINCT ON (org_id) org_id, org_name FROM wr_record WHERE del_flag=0 ORDER BY org_id) r " +
            "       ON r.org_id = nb.org_id " +
            "WHERE nb.del_flag = 'N'";
        return jdbc.query(sql, Collections.emptyMap(), (rs, i) -> {
            OrgRow o          = new OrgRow();
            o.orgId           = rs.getLong("org_id");
            o.orgName         = rs.getString("org_name");
            o.cityCenterIds   = parseIds(rs.getString("city_center_ids"));
            o.countyCenterIds = parseIds(rs.getString("county_center_ids"));
            return o;
        });
    }

    private OrgRow loadSingleOrg(long orgId) {
        String sql =
            "SELECT nb.org_id, nb.city_center_ids, nb.county_center_ids, " +
            "       r.org_name " +
            "FROM qc_network_build nb " +
            "LEFT JOIN (SELECT DISTINCT ON (org_id) org_id, org_name FROM wr_record WHERE del_flag=0 ORDER BY org_id) r " +
            "       ON r.org_id = nb.org_id " +
            "WHERE nb.org_id = :orgId AND nb.del_flag = 'N'";
        List<OrgRow> rows = jdbc.query(sql, p("orgId", orgId), (rs, i) -> {
            OrgRow o          = new OrgRow();
            o.orgId           = rs.getLong("org_id");
            o.orgName         = rs.getString("org_name");
            o.cityCenterIds   = parseIds(rs.getString("city_center_ids"));
            o.countyCenterIds = parseIds(rs.getString("county_center_ids"));
            return o;
        });
        return rows.isEmpty() ? null : rows.get(0);
    }

    // region lookups — same as DsQcQuery

    private List<RegionRow> loadCityRegions() {
        return jdbc.query(
            "SELECT id, name FROM dw_region WHERE tree_type = 'city' ORDER BY sort_order",
            Collections.emptyMap(),
            (rs, i) -> { RegionRow r = new RegionRow(); r.id = rs.getInt("id"); r.name = rs.getString("name"); return r; }
        );
    }

    private Map<Integer, List<RegionRow>> loadCountiesByParentAll() {
        List<RegionRow> rows = jdbc.query(
            "SELECT id, parent_id, name FROM dw_region WHERE tree_type = 'county' AND level = 3 ORDER BY parent_id, sort_order",
            Collections.emptyMap(),
            (rs, i) -> { RegionRow r = new RegionRow(); r.id = rs.getInt("id"); r.parentId = rs.getInt("parent_id"); r.name = rs.getString("name"); return r; }
        );
        Map<Integer, List<RegionRow>> map = new LinkedHashMap<>();
        for (RegionRow row : rows) map.computeIfAbsent(row.parentId, k -> new ArrayList<>()).add(row);
        return map;
    }

    private List<RegionRow> loadCountiesByParent(int parentId) {
        return jdbc.query(
            "SELECT id, name FROM dw_region WHERE tree_type = 'county' AND parent_id = :pid AND level = 3 ORDER BY sort_order",
            p("pid", parentId),
            (rs, i) -> { RegionRow r = new RegionRow(); r.id = rs.getInt("id"); r.name = rs.getString("name"); return r; }
        );
    }

    private Map<Integer, String> loadCountyParentNames() {
        Map<Integer, String> map = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM dw_region WHERE tree_type = 'county' AND level = 2 ORDER BY sort_order",
            Collections.<String, Object>emptyMap(), (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                map.put(rs.getInt("id"), rs.getString("name")));
        return map;
    }

    private String loadCountyParentName(int countyParentId) {
        List<String> rows = jdbc.query(
            "SELECT name FROM dw_region WHERE id = :id AND tree_type = 'county' AND level = 2",
            p("id", countyParentId), (rs, i) -> rs.getString("name"));
        return rows.isEmpty() ? "未知城市" : rows.get(0);
    }

    // JSON array parser

    private Set<Integer> parseIds(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptySet();
        String s = json.trim().replaceAll("[\\[\\]\\s]", "");
        if (s.isEmpty()) return Collections.emptySet();
        Set<Integer> result = new LinkedHashSet<>();
        for (String part : s.split(",")) {
            part = part.trim();
            if (!part.isEmpty()) {
                try { result.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    private static Map<String, Object> p(String key, Object value) {
        return Collections.singletonMap(key, value);
    }

    // inner classes

    private static class OrgRow {
        long         orgId;
        String       orgName;
        Set<Integer> cityCenterIds   = Collections.emptySet();
        Set<Integer> countyCenterIds = Collections.emptySet();
    }

    private static class RegionRow {
        int    id;
        int    parentId;
        String name;
    }
}
