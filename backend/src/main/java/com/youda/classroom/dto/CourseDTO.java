package com.youda.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程新增/修改请求参数
 */
@Data
public class CourseDTO {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String courseCode;

    @NotNull(message = "授课教师不能为空")
    private Long teacherId;

    private String description;

    private BigDecimal credit;

    private String semester;

    private Integer status;
}
