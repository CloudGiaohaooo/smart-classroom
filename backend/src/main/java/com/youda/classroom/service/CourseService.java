package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.dto.CourseDTO;
import com.youda.classroom.entity.Course;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程服务：课程管理模块
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    /** 课程全量列表缓存键（热点数据：下拉选择、看板频繁读取） */
    private static final String COURSE_LIST_CACHE_KEY = "cache:course:all";

    private final CourseMapper courseMapper;
    private final RedisCacheService cacheService;

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
        return cacheService.getOrLoad(COURSE_LIST_CACHE_KEY,
                new TypeReference<List<Course>>() {}, courseListTtl,
                () -> courseMapper.selectList(null));
    }

    public void create(CourseDTO dto) {
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        course.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        courseMapper.insert(course);
        cacheService.evict(COURSE_LIST_CACHE_KEY);
    }

    public void update(Long id, CourseDTO dto) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        BeanUtils.copyProperties(dto, course);
        courseMapper.updateById(course);
        cacheService.evict(COURSE_LIST_CACHE_KEY);
    }

    public void delete(Long id) {
        courseMapper.deleteById(id);
        cacheService.evict(COURSE_LIST_CACHE_KEY);
    }
}
