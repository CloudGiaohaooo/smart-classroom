package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.vo.SubmissionVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作业提交 Mapper
 */
public interface SubmissionMapper extends BaseMapper<Submission> {

    /**
     * 多表查询：提交 LEFT JOIN 作业、课程、学生三张表，联表查出作业标题、课程名与学生姓名
     */
    @Select("<script>" +
            "SELECT s.id, s.assignment_id, s.student_id, s.content, s.file_url, s.submit_time, s.score, s.feedback, s.status, s.grade_time, " +
            "       a.title AS assignment_title, a.total_score, " +
            "       c.course_name, " +
            "       u.real_name AS student_name " +
            "FROM submission s " +
            "LEFT JOIN assignment a ON s.assignment_id = a.id " +
            "LEFT JOIN course c ON a.course_id = c.id " +
            "LEFT JOIN sys_user u ON s.student_id = u.id " +
            "<where>" +
            "  <if test='assignmentId != null'> AND s.assignment_id = #{assignmentId} </if>" +
            "  <if test='studentId != null'> AND s.student_id = #{studentId} </if>" +
            "  <if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            "  <if test='status != null and status != \"\"'> AND s.status = #{status} </if>" +
            "</where>" +
            "ORDER BY s.id DESC" +
            "</script>")
    IPage<SubmissionVO> selectSubmissionPage(Page<?> page,
                                             @Param("assignmentId") Long assignmentId,
                                             @Param("studentId") Long studentId,
                                             @Param("status") String status,
                                             @Param("teacherId") Long teacherId);

    /**
     * 查询某教师授课课程下所有已批阅提交（评语推荐全局补齐用），限定本人课程防跨教师评语泄露
     */
    @Select("SELECT s.* FROM submission s " +
            "JOIN assignment a ON s.assignment_id = a.id " +
            "JOIN course c ON a.course_id = c.id " +
            "WHERE c.teacher_id = #{teacherId} " +
            "  AND s.feedback IS NOT NULL AND s.feedback <> ''")
    List<Submission> selectGradedByTeacher(@Param("teacherId") Long teacherId);
}
