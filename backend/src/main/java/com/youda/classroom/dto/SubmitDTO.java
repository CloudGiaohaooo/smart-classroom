package com.youda.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生提交作业请求参数
 */
@Data
public class SubmitDTO {

    @NotNull(message = "作业不能为空")
    private Long assignmentId;

    private String content;

    private String fileUrl;
}
