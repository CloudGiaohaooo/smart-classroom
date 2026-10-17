package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.youda.classroom.cache.CacheKeys;
import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.dto.CourseDTO;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.CourseResource;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.CourseResourceMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程服务：课程管理模块
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final CourseResourceMapper resourceMapper;
    private final RedisCacheService cacheService;
    private final CourseOwnershipGuard ownershipGuard;

    @Value("${cache.course-list-ttl}")
    private long courseListTtl;

    /**
     * 分页查询课程（联表带出教师姓名）
     */
    public IPage<CourseVO> page(long current, long size, String keyword, Long teacherId) {
        return courseMapper.selectCoursePage(new Page<>(current, size), keyword, teacherId);
    }

    /**
     * 全部课程（供下拉选择）：走 Redis 缓存，落地穿透/击穿/雪崩三策略，Redis 宕机自动降级
     */
    public List<Course> listAll() {
        return cacheService.getOrLoad(CacheKeys.COURSE_LIST,
                new TypeReference<List<Course>>() {}, courseListTtl,
                () -> courseMapper.selectList(null));
    }

    public void create(CourseDTO dto) {
        Course course = new Course();
        course.setCourseName(dto.getCourseName());
        course.setCourseCode(dto.getCourseCode());
        course.setDescription(dto.getDescription());
        course.setCredit(dto.getCredit());
        course.setSemester(dto.getSemester());
        course.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        // P0-B：授课教师归属——非管理员只能为自己建课，防止教师代建他人课程；管理员可代建（沿用 DTO 的 teacherId）
        course.setTeacherId("ADMIN".equals(UserContext.getRole()) ? dto.getTeacherId() : UserContext.getUserId());
        courseMapper.insert(course);
        cacheService.evict(CacheKeys.COURSE_LIST);
        cacheService.evict(CacheKeys.STATS_OVERVIEW);
    }

    public void update(Long id, CourseDTO dto) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        // P0-B：仅管理员或该课程的授课教师可修改
        ownershipGuard.assertCourseOwned(course);
        // 显式字段拷贝，未传的可选字段不覆盖（修复 BeanUtils 全量拷贝把未传字段清空的问题）
        course.setCourseName(dto.getCourseName());
        if (dto.getCourseCode() != null) {
            course.setCourseCode(dto.getCourseCode());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getCredit() != null) {
            course.setCredit(dto.getCredit());
        }
        if (dto.getSemester() != null) {
            course.setSemester(dto.getSemester());
        }
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
        // 课程归属仅管理员可变更，且需显式传入 teacherId
        if ("ADMIN".equals(UserContext.getRole()) && dto.getTeacherId() != null) {
            course.setTeacherId(dto.getTeacherId());
        }
        courseMapper.updateById(course);
        cacheService.evict(CacheKeys.COURSE_LIST);
        cacheService.evict(CacheKeys.STATS_OVERVIEW);
    }

    public void delete(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        // P0-B：仅管理员或该课程的授课教师可删除
        ownershipGuard.assertCourseOwned(course);
        Long assignmentCount = assignmentMapper.selectCount(
                new LambdaQueryWrapper<Assignment>().eq(Assignment::getCourseId, id));
        Long resourceCount = resourceMapper.selectCount(
                new LambdaQueryWrapper<CourseResource>().eq(CourseResource::getCourseId, id));
        if ((assignmentCount != null && assignmentCount > 0) || (resourceCount != null && resourceCount > 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该课程下存在作业或资源，请先清理后再删除");
        }
        courseMapper.deleteById(id);
        cacheService.evict(CacheKeys.COURSE_LIST);
        cacheService.evict(CacheKeys.STATS_OVERVIEW);
    }
}
