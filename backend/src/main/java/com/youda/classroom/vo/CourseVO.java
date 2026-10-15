package com.youda.classroom.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程视图对象（含联表查出的教师姓名）
 */
@Data
public class CourseVO {

    private Long id;

    private String courseName;

    private String courseCode;

    private Long teacherId;

    private String teacherName;

    private String description;

    private BigDecimal credit;

    private String semester;

    private Integer status;

    private LocalDateTime createTime;
}
