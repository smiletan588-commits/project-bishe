package com.smartpm.controller;

import com.smartpm.common.result.R;
import com.smartpm.service.AnalyticsService;
import com.smartpm.vo.AnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public R<AnalyticsVO> overview() {
        return R.ok(analyticsService.getOverview());
    }
}
