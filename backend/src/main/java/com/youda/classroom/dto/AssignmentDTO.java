package com.youda.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业新增/修改请求参数
 */
@Data
public class AssignmentDTO {

    @NotNull(message = "所属课程不能为空")
    private Long courseId;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String content;

    private LocalDateTime deadline;

    private BigDecimal totalScore;
}
