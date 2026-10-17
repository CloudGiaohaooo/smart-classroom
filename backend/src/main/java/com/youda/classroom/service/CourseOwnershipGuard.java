package com.youda.classroom.service;

import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.entity.Course;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 课程归属校验（P0-B 横向越权修复）。
 *
 * 统一入口断言「当前用户对目标课程有操作权」，供批阅 / 课程 / 作业 / 资源的写操作复用，
 * 避免出现「A 教师批改 B 教师的作业、改 B 的课程、删 B 的资源」这类横向越权。
 *
 * 规则：
 *  - ADMIN：放行（可操作任意课程）；
 *  - TEACHER：仅能操作自己授课的课程（course.teacher_id == 当前用户 id）；
 *  - 其他角色：无写权限，直接拒绝（防御：即使 @PreAuthorize 被绕过也不越权）。
 */
@Component
@RequiredArgsConstructor
public class CourseOwnershipGuard {

    private final CourseMapper courseMapper;

    /** 按课程 id 校验（内部再查课程，适用于调用方尚未持有课程实体的场景） */
    public void assertCourseOwned(Long courseId) {
        if (courseId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课程不能为空");
        }
        assertCourseOwned(courseMapper.selectById(courseId));
    }

    /** 按已加载的课程实体校验（调用方已查过课程时复用，避免重复查询） */
    public void assertCourseOwned(Course course) {
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        String role = UserContext.getRole();
        if ("ADMIN".equals(role)) {
            return;
        }
        if (!"TEACHER".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限操作该课程");
        }
        Long userId = UserContext.getUserId();
        if (userId == null || !userId.equals(course.getTeacherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作其他教师的课程");
        }
    }
}
