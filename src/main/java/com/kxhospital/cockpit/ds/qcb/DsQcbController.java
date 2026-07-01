package com.kxhospital.cockpit.ds.qcb;

import com.kxhospital.cockpit.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ds/qcb")
@CrossOrigin
@RequiredArgsConstructor
public class DsQcbController {

    private final DsQcbQuery query;

    @GetMapping("/orgs")
    public R<?> orgs() {
        return R.ok(query.listOrgSummaries());
    }

    @GetMapping("/province")
    public R<DsQcbProvinceVO> province() {
        return R.ok(query.buildProvince());
    }

    @GetMapping("/city/{cityId}")
    public R<DsQcbCityDrillDownVO> city(@PathVariable Integer cityId) {
        if (cityId < 101 || cityId > 111) {
            return R.fail(400, "cityId 取值范围 101-111");
        }
        return R.ok(query.buildCityDrillDown(cityId));
    }

    @GetMapping("/org/{orgId}")
    public R<DsQcbOrgDetailVO> org(@PathVariable Long orgId) {
        DsQcbOrgDetailVO detail = query.buildOrgDetail(orgId);
        if (detail == null) {
            return R.fail(404, "该机构无 qc_network_build 数据");
        }
        return R.ok(detail);
    }
}
