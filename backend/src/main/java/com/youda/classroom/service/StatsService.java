package com.youda.classroom.service;

import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.CourseResource;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.CourseResourceMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 统计数据服务：为仪表盘提供各类聚合统计
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseResourceMapper resourceMapper;

    public StatsVO overview() {
        StatsVO vo = new StatsVO();
        vo.setCourseCount(courseMapper.selectCount(null));
        vo.setTeacherCount(userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getRole, "TEACHER")));
        vo.setStudentCount(userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getRole, "STUDENT")));
        vo.setAssignmentCount(assignmentMapper.selectCount(null));
        vo.setSubmissionCount(submissionMapper.selectCount(null));
        vo.setPendingGradeCount(submissionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStatus, "SUBMITTED")));
        vo.setResourceCount(resourceMapper.selectCount(null));
        vo.setCourseAssignmentStats(assignmentMapper.countAssignmentByCourse());
        return vo;
    }
}
