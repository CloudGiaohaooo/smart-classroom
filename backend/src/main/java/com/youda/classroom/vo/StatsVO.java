package com.youda.classroom.vo;

import lombok.Data;

import java.util.List;

/**
 * 仪表盘统计数据
 */
@Data
public class StatsVO {

    private Long courseCount;

    private Long teacherCount;

    private Long studentCount;

    private Long assignmentCount;

    private Long submissionCount;

    private Long pendingGradeCount;

    private Long resourceCount;

    /** 各课程作业数量（用于前端图表） */
    private List<CourseAssignmentStatVO> courseAssignmentStats;
}
