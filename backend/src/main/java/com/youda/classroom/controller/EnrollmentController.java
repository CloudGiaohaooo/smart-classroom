package com.youda.classroom.controller;

import com.youda.classroom.common.Result;
import com.youda.classroom.service.EnrollmentService;
import com.youda.classroom.vo.CourseVO;
import com.youda.classroom.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 选课接口（Phase 18 选课模型）
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{courseId}")
    @PreAuthorize("hasAuthority('enrollment:write')")
    public Result<Void> enroll(@PathVariable Long courseId) {
        enrollmentService.enroll(courseId);
        return Result.success("选课成功", null);
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAuthority('enrollment:write')")
    public Result<Void> unenroll(@PathVariable Long courseId) {
        enrollmentService.unenroll(courseId);
        return Result.success("退课成功", null);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('enrollment:read')")
    public Result<List<CourseVO>> my() {
        return Result.success(enrollmentService.myCourses());
    }

    @GetMapping("/course/{courseId}/students")
    @PreAuthorize("hasAuthority('enrollment:read')")
    public Result<List<UserVO>> students(@PathVariable Long courseId) {
        return Result.success(enrollmentService.listStudents(courseId));
    }
}
