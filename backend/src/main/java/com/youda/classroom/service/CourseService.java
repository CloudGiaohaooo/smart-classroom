package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.dto.CourseDTO;
import com.youda.classroom.entity.Course;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程服务：课程管理模块
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;

    /**
     * 分页查询课程（联表带出教师姓名）
     */
    public IPage<CourseVO> page(long current, long size, String keyword, Long teacherId) {
        return courseMapper.selectCoursePage(new Page<>(current, size), keyword, teacherId);
    }

    /**
     * 全部课程（供下拉选择）
     */
    public List<Course> listAll() {
        return courseMapper.selectList(null);
    }

    public void create(CourseDTO dto) {
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        course.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        courseMapper.insert(course);
    }

    public void update(Long id, CourseDTO dto) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        BeanUtils.copyProperties(dto, course);
        courseMapper.updateById(course);
    }

    public void delete(Long id) {
        courseMapper.deleteById(id);
    }
}
