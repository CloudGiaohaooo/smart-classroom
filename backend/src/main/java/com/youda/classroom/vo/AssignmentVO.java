package com.youda.classroom.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业视图对象（含课程名、提交数、已批阅数）
 */
@Data
public class AssignmentVO {

    private Long id;

    private Long courseId;

    private String courseName;

    private String title;

    private String content;

    private LocalDateTime deadline;

    private BigDecimal totalScore;

    private LocalDateTime createTime;

    private Integer submissionCount;

    private Integer gradedCount;
}
