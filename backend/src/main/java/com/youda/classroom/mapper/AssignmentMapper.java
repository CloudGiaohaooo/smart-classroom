package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.vo.AssignmentVO;
import com.youda.classroom.vo.CourseAssignmentStatVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作业 Mapper
 */
public interface AssignmentMapper extends BaseMapper<Assignment> {

    /**
     * 多表查询：作业 LEFT JOIN 课程 + 子查询统计提交数，
     * 演示 LEFT JOIN 与聚合子查询结合的场景
     */
    @Select("<script>" +
            "SELECT a.id, a.course_id, a.title, a.content, a.deadline, a.total_score, a.create_time, " +
            "       c.course_name, " +
            "       (SELECT COUNT(*) FROM submission s WHERE s.assignment_id = a.id) AS submission_count, " +
            "       (SELECT COUNT(*) FROM submission s WHERE s.assignment_id = a.id AND s.status = 'GRADED') AS graded_count " +
            "FROM assignment a LEFT JOIN course c ON a.course_id = c.id " +
            "<where>" +
            "  <if test='courseId != null'> AND a.course_id = #{courseId} </if>" +
            "  <if test='keyword != null and keyword != \"\"'> AND a.title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "  <if test='studentId != null'> AND EXISTS (SELECT 1 FROM course_enrollment ce WHERE ce.course_id = a.course_id AND ce.student_id = #{studentId}) </if>" +
            "</where>" +
            "ORDER BY a.id DESC" +
            "</script>")
    IPage<AssignmentVO> selectAssignmentPage(Page<?> page,
                                             @Param("courseId") Long courseId,
                                             @Param("keyword") String keyword,
                                             @Param("studentId") Long studentId);

    /**
     * 按课程分组统计作业数量（LEFT JOIN + GROUP BY，用于仪表盘图表）
     */
    @Select("SELECT c.course_name AS course_name, COUNT(a.id) AS assignment_count " +
            "FROM course c LEFT JOIN assignment a ON a.course_id = c.id " +
            "GROUP BY c.id, c.course_name " +
            "ORDER BY assignment_count DESC")
    List<CourseAssignmentStatVO> countAssignmentByCourse();
}
