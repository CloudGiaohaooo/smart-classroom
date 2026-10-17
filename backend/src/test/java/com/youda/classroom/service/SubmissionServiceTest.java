package com.youda.classroom.service;

import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.dto.SubmitDTO;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提交服务单测：验证「选课门禁」——学生提交前必须已选修作业所属课程，非学生角色不适用该门禁。
 * （选课门禁的判定逻辑 isEnrolled/assertEnrolled 在 EnrollmentServiceTest 中单独覆盖）
 */
class SubmissionServiceTest {

    private SubmissionMapper submissionMapper;
    private AssignmentMapper assignmentMapper;
    private EnrollmentService enrollmentService;
    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionMapper = mock(SubmissionMapper.class);
        assignmentMapper = mock(AssignmentMapper.class);
        enrollmentService = mock(EnrollmentService.class);
        submissionService = new SubmissionService(submissionMapper, assignmentMapper,
                mock(RedisCacheService.class), mock(CourseOwnershipGuard.class), enrollmentService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private Assignment assignment() {
        Assignment a = new Assignment();
        a.setId(1L);
        a.setCourseId(10L);
        a.setDeadline(null);
        return a;
    }

    private SubmitDTO dto() {
        SubmitDTO dto = new SubmitDTO();
        dto.setAssignmentId(1L);
        dto.setContent("提交内容");
        return dto;
    }

    @Test
    void studentSubmitInvokesEnrollmentGate() {
        UserContext.set(new UserContext.CurrentUser(2L, "student", "STUDENT"));
        when(assignmentMapper.selectById(1L)).thenReturn(assignment());
        when(submissionMapper.selectCount(any())).thenReturn(0L);

        submissionService.submit(dto());

        verify(enrollmentService).assertEnrolled(10L);
    }

    @Test
    void adminSubmitSkipsEnrollmentGate() {
        UserContext.set(new UserContext.CurrentUser(1L, "admin", "ADMIN"));
        when(assignmentMapper.selectById(1L)).thenReturn(assignment());
        when(submissionMapper.selectCount(any())).thenReturn(0L);

        submissionService.submit(dto());

        verify(enrollmentService, never()).assertEnrolled(anyLong());
    }
}
