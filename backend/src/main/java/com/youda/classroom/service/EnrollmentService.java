package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.CourseEnrollment;
import com.youda.classroom.mapper.CourseEnrollmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.CourseVO;
import com.youda.classroom.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课服务（Phase 18 选课模型）。
 *
 * 职责：
 *  1. 学生自主选课 / 退课（写操作，最小权限 enrollment:write）；
 *  2. 查询「我选的课程」（学生视角）与「课程选了哪些学生」（教师/管理员视角）；
 *  3. 提供 isEnrolled / assertEnrolled 供提交、作业可见性等做「选课门禁」。
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final CourseOwnershipGuard ownershipGuard;

    /** 学生自主选课（仅 STUDENT 角色可调） */
    public void enroll(Long courseId) {
        if (!"STUDENT".equals(UserContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅学生可自主选课");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        CourseEnrollment ce = new CourseEnrollment();
        ce.setCourseId(courseId);
        ce.setStudentId(UserContext.getUserId());
        ce.setEnrollTime(LocalDateTime.now());
        try {
            enrollmentMapper.insert(ce);
        } catch (DuplicateKeyException e) {
            // 唯一索引 (course_id, student_id) 兜底：重复选课给出友好提示
            throw new BusinessException(ResultCode.BAD_REQUEST, "已选该课程，请勿重复选课");
        }
    }

    /** 学生退课（仅 STUDENT 角色可调） */
    public void unenroll(Long courseId) {
        if (!"STUDENT".equals(UserContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅学生可退课");
        }
        CourseEnrollment ce = enrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getCourseId, courseId)
                .eq(CourseEnrollment::getStudentId, UserContext.getUserId()));
        if (ce == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未选修该课程");
        }
        enrollmentMapper.deleteById(ce.getId());
    }

    /** 查询「我选的课程」（当前学生视角） */
    public List<CourseVO> myCourses() {
        return enrollmentMapper.selectEnrolledCourses(UserContext.getUserId());
    }

    /** 查询某课程的选课学生（仅管理员或该课程授课教师可调） */
    public List<UserVO> listStudents(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        // 教师仅能查看自己授课课程的选课名单，管理员全量，其余角色拒绝
        ownershipGuard.assertCourseOwned(course);
        return enrollmentMapper.selectEnrolledStudents(courseId);
    }

    /** 当前用户是否已选修某课程 */
    public boolean isEnrolled(Long courseId) {
        Long studentId = UserContext.getUserId();
        if (studentId == null || courseId == null) {
            return false;
        }
        Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getCourseId, courseId)
                .eq(CourseEnrollment::getStudentId, studentId));
        return count != null && count > 0;
    }

    /** 选课门禁：未选修则拒绝（供提交作业等操作复用） */
    public void assertEnrolled(Long courseId) {
        if (!isEnrolled(courseId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未选修该课程，无法操作");
        }
    }
}
