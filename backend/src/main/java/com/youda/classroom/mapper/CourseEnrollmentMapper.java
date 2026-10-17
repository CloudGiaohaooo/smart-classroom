package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youda.classroom.entity.CourseEnrollment;
import com.youda.classroom.vo.CourseVO;
import com.youda.classroom.vo.UserVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 选课 Mapper
 */
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollment> {

    /**
     * 查询某学生已选课程（JOIN 课程与教师，联表带出教师姓名）
     */
    @Select("SELECT c.id, c.course_name, c.course_code, c.teacher_id, c.description, c.credit, c.semester, c.status, c.create_time, " +
            "       u.real_name AS teacher_name " +
            "FROM course_enrollment ce " +
            "JOIN course c ON ce.course_id = c.id " +
            "LEFT JOIN sys_user u ON c.teacher_id = u.id " +
            "WHERE ce.student_id = #{studentId} " +
            "ORDER BY ce.id DESC")
    List<CourseVO> selectEnrolledCourses(@Param("studentId") Long studentId);

    /**
     * 查询某课程的选课学生（JOIN 用户表带出姓名，不含密码等敏感字段）
     */
    @Select("SELECT u.id, u.username, u.real_name, u.role " +
            "FROM course_enrollment ce " +
            "JOIN sys_user u ON ce.student_id = u.id " +
            "WHERE ce.course_id = #{courseId} " +
            "ORDER BY u.id")
    List<UserVO> selectEnrolledStudents(@Param("courseId") Long courseId);
}
