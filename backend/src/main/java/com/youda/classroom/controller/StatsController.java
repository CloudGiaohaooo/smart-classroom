package com.youda.classroom.controller;

import com.youda.classroom.common.Result;
import com.youda.classroom.service.BehaviorService;
import com.youda.classroom.service.StatsService;
import com.youda.classroom.vo.BehaviorDashboardVO;
import com.youda.classroom.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘统计接口
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final BehaviorService behaviorService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('stats:read')")
    public Result<StatsVO> overview() {
        return Result.success(statsService.overview());
    }

    /**
     * 学习行为看板（Phase 4 创新点 B）：提交率趋势 + 资源热力 + 学习预警列表
     */
    @GetMapping("/behavior")
    @PreAuthorize("hasAuthority('stats:read')")
    public Result<BehaviorDashboardVO> behavior() {
        return Result.success(behaviorService.dashboard());
    }
}
