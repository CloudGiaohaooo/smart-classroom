package com.youda.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交实体（对应 submission 表）
 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long studentId;

    private String content;

    private String fileUrl;

    private LocalDateTime submitTime;

    private BigDecimal score;

    private String feedback;

    /** 状态：SUBMITTED 已提交 / GRADED 已批阅 */
    private String status;

    private LocalDateTime gradeTime;
}
