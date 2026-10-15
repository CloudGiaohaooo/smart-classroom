package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.entity.Course;
import com.youda.classroom.vo.CourseVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程 Mapper
 */
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 多表查询：课程 LEFT JOIN 用户，联表查出教师姓名，并支持分页与关键词过滤
     */
    @Select("<script>" +
            "SELECT c.id, c.course_name, c.course_code, c.teacher_id, c.description, c.credit, c.semester, c.status, c.create_time, " +
            "       u.real_name AS teacher_name " +
            "FROM course c LEFT JOIN sys_user u ON c.teacher_id = u.id " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (c.course_name LIKE CONCAT('%', #{keyword}, '%') OR c.course_code LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            "</where>" +
            "ORDER BY c.id DESC" +
            "</script>")
    IPage<CourseVO> selectCoursePage(Page<?> page,
                                     @Param("keyword") String keyword,
                                     @Param("teacherId") Long teacherId);
}
