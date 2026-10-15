package com.youda.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体（对应 course 表）
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String courseName;

    private String courseCode;

    /** 授课教师 id，关联 sys_user.id */
    private Long teacherId;

    private String description;

    private BigDecimal credit;

    private String semester;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
