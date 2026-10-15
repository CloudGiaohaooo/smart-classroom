package com.youda.classroom.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交视图对象（含作业标题、课程名、学生姓名）
 */
@Data
public class SubmissionVO {

    private Long id;

    private Long assignmentId;

    private String assignmentTitle;

    private Long studentId;

    private String studentName;

    private String courseName;

    private String content;

    private String fileUrl;

    private LocalDateTime submitTime;

    private BigDecimal score;

    private BigDecimal totalScore;

    private String feedback;

    private String status;

    private LocalDateTime gradeTime;
}
