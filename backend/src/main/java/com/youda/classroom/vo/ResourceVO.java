package com.youda.classroom.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程资源视图对象（含课程名、上传人姓名）
 */
@Data
public class ResourceVO {

    private Long id;

    private Long courseId;

    private String courseName;

    private String resourceName;

    private String resourceType;

    private String fileUrl;

    private Long fileSize;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime uploadTime;
}
