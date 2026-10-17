package com.youda.classroom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师批阅作业请求参数
 */
@Data
public class GradeDTO {

    @NotNull(message = "得分不能为空")
    @DecimalMin(value = "0.0", message = "得分不能为负数")
    private BigDecimal score;

    @Size(max = 1000, message = "反馈不能超过 1000 字")
    private String feedback;
}
