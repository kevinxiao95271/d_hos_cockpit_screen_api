package com.kxhospital.cockpit.ds;

import com.kxhospital.cockpit.common.R;
import com.kxhospital.cockpit.ds.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/ds/qc")
@CrossOrigin
@RequiredArgsConstructor
public class DsQcController {

    private final DsQcQuery query;

    @GetMapping("/regions")
    public R<DsRegionTreeVO> regions() {
        return R.ok(query.buildRegionTree());
    }

    @GetMapping("/tasks")
    public R<List<DsTaskOptionVO>> tasks() {
        return R.ok(query.listTasks());
    }

    @GetMapping("/orgs")
    public R<List<DsOrgSummaryVO>> orgs(@RequestParam Long taskId) {
        return R.ok(query.listOrgSummaries(taskId));
    }

    @GetMapping("/province")
    public R<DsProvinceMapVO> province(@RequestParam              Long   taskId,
                                       @RequestParam(required = false) String orgIds) {
        return R.ok(query.buildProvinceMap(taskId, parseOrgIds(orgIds)));
    }

    @GetMapping("/city/{cityId}")
    public R<DsCityDrillDownVO> city(@PathVariable              Integer cityId,
                                     @RequestParam              Long    taskId,
                                     @RequestParam(required = false) String  orgIds) {
        if (cityId < 101 || cityId > 111) {
            return R.fail(400, "cityId 取值范围 101-111");
        }
        return R.ok(query.buildCityDrillDown(taskId, cityId, parseOrgIds(orgIds)));
    }

    @GetMapping("/org/{orgId}")
    public R<DsOrgDetailVO> org(@PathVariable Long orgId,
                                @RequestParam  Long taskId) {
        DsOrgDetailVO detail = query.buildOrgDetail(taskId, orgId);
        if (detail == null) {
            return R.fail(404, "该机构在本任务中无最终态上报记录");
        }
        return R.ok(detail);
    }

    private List<Long> parseOrgIds(String orgIds) {
        if (orgIds == null || orgIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String s : orgIds.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) {
                try {
                    result.add(Long.parseLong(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }
}
