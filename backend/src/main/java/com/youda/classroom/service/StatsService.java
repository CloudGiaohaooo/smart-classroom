package com.youda.classroom.service;

import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.CourseResourceMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 统计数据服务：为仪表盘提供各类聚合统计
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    /** 看板聚合统计缓存键（热点数据：仪表盘首页频繁读取，涉及多表 COUNT） */
    public static final String STATS_CACHE_KEY = "cache:stats:overview";

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseResourceMapper resourceMapper;
    private final RedisCacheService cacheService;

    @Value("${cache.stats-ttl}")
    private long statsTtl;

    public StatsVO overview() {
        // 看板聚合涉及多表 COUNT，属于典型热点读，走 Redis 缓存；TTL 到期自动刷新保证最终一致
        return cacheService.getOrLoad(STATS_CACHE_KEY,
                new TypeReference<StatsVO>() {}, statsTtl,
                this::loadOverview);
    }

    private StatsVO loadOverview() {
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
