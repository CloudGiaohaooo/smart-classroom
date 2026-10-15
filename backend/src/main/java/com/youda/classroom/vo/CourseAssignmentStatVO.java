package com.youda.classroom.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程维度的作业数量统计（用于图表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssignmentStatVO {

    private String courseName;

    private Long assignmentCount;
}
