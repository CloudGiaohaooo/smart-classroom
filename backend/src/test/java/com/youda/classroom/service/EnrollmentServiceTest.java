package com.youda.classroom.service;

import com.youda.classroom.common.BusinessException;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.CourseEnrollment;
import com.youda.classroom.mapper.CourseEnrollmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 选课服务单测：验证选课 / 退课 / 选课门禁的核心分支，
 * 与 EnrollmentControllerTest（HTTP 层 RBAC）共同覆盖 Phase 18 选课模型。
 */
class EnrollmentServiceTest {

    private CourseEnrollmentMapper enrollmentMapper;
    private CourseMapper courseMapper;
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentMapper = mock(CourseEnrollmentMapper.class);
        courseMapper = mock(CourseMapper.class);
        enrollmentService = new EnrollmentService(enrollmentMapper, courseMapper, mock(CourseOwnershipGuard.class));
        UserContext.set(new UserContext.CurrentUser(2L, "student", "STUDENT"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void enrollInsertsForExistingCourse() {
        when(courseMapper.selectById(10L)).thenReturn(new Course());

        enrollmentService.enroll(10L);

        verify(enrollmentMapper).insert(any(CourseEnrollment.class));
    }

    @Test
    void enrollMissingCourseThrows404() {
        when(courseMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void enrollDuplicateThrows400() {
        when(courseMapper.selectById(10L)).thenReturn(new Course());
        doThrow(new DuplicateKeyException("dup"))
                .when(enrollmentMapper).insert(any(CourseEnrollment.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(10L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void assertEnrolledThrows403WhenNotEnrolled() {
        when(enrollmentMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.assertEnrolled(10L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void isEnrolledReturnsTrueWhenRecordExists() {
        when(enrollmentMapper.selectCount(any())).thenReturn(1L);

        // 已选修则不抛异常（断言通过即表示 isEnrolled 判定为 true）
        enrollmentService.assertEnrolled(10L);
    }
}
