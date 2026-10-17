package com.youda.classroom.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 每名学生每类行为的最近发生时间（预警规则计算的中间聚合结果）
 */
@Data
public class BehaviorLastVO {

    private Long studentId;

    private String behaviorType;

    private LocalDateTime lastTime;
}
