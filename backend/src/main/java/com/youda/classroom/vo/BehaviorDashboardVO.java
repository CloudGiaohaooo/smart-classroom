package com.youda.classroom.vo;

import lombok.Data;

import java.util.List;

/**
 * 学习行为看板（Phase 4 创新点 B）：
 * 提交率趋势 + 资源访问热力 + 学习预警列表，全部来源于 AOP 埋点表 student_behavior。
 */
@Data
public class BehaviorDashboardVO {

    /** 近 N 天提交量趋势 */
    private List<SubmitTrendPointVO> submitTrend;

    /** 各课程资源访问热度 */
    private List<ResourceHeatVO> resourceHeat;

    /** 学习预警列表 */
    private List<StudentWarningVO> warningList;
}
