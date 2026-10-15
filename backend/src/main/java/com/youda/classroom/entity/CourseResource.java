package com.youda.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程资源实体（对应 course_resource 表）
 */
@Data
@TableName("course_resource")
public class CourseResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String resourceName;

    /** 资源类型：VIDEO/PDF/DOC/PPT/OTHER */
    private String resourceType;

    private String fileUrl;

    private Long fileSize;

    private Long uploaderId;

    private LocalDateTime uploadTime;
}
