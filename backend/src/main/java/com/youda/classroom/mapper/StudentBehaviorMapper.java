package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youda.classroom.entity.StudentBehavior;
import com.youda.classroom.vo.BehaviorLastVO;
import com.youda.classroom.vo.ResourceHeatVO;
import com.youda.classroom.vo.SubmitTrendPointVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生行为埋点 Mapper（Phase 4 创新点 B）
 */
public interface StudentBehaviorMapper extends BaseMapper<StudentBehavior> {

    /**
     * 近 N 天提交量趋势：仅统计 SUBMIT_ASSIGNMENT 事件，按天分组
     */
    @Select("SELECT DATE_FORMAT(occur_time, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM student_behavior " +
            "WHERE behavior_type = 'SUBMIT_ASSIGNMENT' " +
            "  AND occur_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(occur_time, '%Y-%m-%d') " +
            "ORDER BY date ASC")
    List<SubmitTrendPointVO> selectSubmitTrend(@Param("days") int days);

    /**
     * 各课程资源访问热度：VIEW_RESOURCE 事件按目标课程分组，取 TopN
     */
    @Select("SELECT c.course_name AS name, COUNT(*) AS count " +
            "FROM student_behavior b JOIN course c ON b.target_id = c.id " +
            "WHERE b.behavior_type = 'VIEW_RESOURCE' " +
            "GROUP BY b.target_id, c.course_name " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<ResourceHeatVO> selectResourceHeat(@Param("limit") int limit);

    /**
     * 每名学生每类行为的最近发生时间（用于预警规则判断）
     */
    @Select("SELECT student_id, behavior_type, MAX(occur_time) AS last_time " +
            "FROM student_behavior " +
            "WHERE behavior_type IN ('LOGIN', 'SUBMIT_ASSIGNMENT') " +
            "GROUP BY student_id, behavior_type")
    List<BehaviorLastVO> selectLastTimePerType();
}
