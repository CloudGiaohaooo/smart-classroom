package com.youda.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生行为埋点实体（对应 student_behavior 表，Phase 4 创新点 B）
 */
@Data
@TableName("student_behavior")
public class StudentBehavior {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** 行为类型：LOGIN / VIEW_RESOURCE / SUBMIT_ASSIGNMENT */
    private String behaviorType;

    /** 目标对象 id：VIEW_RESOURCE -> 课程 id；SUBMIT_ASSIGNMENT -> 作业 id；LOGIN 为 null */
    private Long targetId;

    /** 行为描述 */
    private String detail;

    private LocalDateTime occurTime;
}
