package com.kxhospital.cockpit.ds;

import com.kxhospital.cockpit.common.R;
import com.kxhospital.cockpit.ds.vo.DsStatSummaryVO;
import com.kxhospital.cockpit.ds.vo.DsStatTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ds/stat")
@CrossOrigin
@RequiredArgsConstructor
public class DsStatController {

    private final DsStatQuery query;

    @GetMapping("/summary")
    public R<DsStatSummaryVO> summary(@RequestParam Long taskId) {
        return R.ok(query.buildSummary(taskId));
    }

    @GetMapping("/trend")
    public R<DsStatTrendVO> trend() {
        return R.ok(query.buildTrend());
    }
}
