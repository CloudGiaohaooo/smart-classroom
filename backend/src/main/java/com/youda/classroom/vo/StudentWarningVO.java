package com.youda.classroom.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习预警（Phase 4 看板：长期未活跃学生）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentWarningVO {

    private Long studentId;

    private String studentName;

    /** 预警原因，如「长期未登录、长期未提交作业」 */
    private String reason;

    /** 最近活跃时间；从未活跃时为 null */
    private LocalDateTime lastActiveTime;

    /** 已静默天数；从未活跃时为 null */
    private Long inactiveDays;
}
