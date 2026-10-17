package com.youda.classroom.controller;

import com.youda.classroom.common.Result;
import com.youda.classroom.service.StatsService;
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

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('stats:read')")
    public Result<StatsVO> overview() {
        return Result.success(statsService.overview());
    }
}
