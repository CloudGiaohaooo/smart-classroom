package com.youda.classroom.controller;

import com.youda.classroom.config.SecurityConfig;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.PermissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.JwtAuthenticationFilter;
import com.youda.classroom.security.JwtUtil;
import com.youda.classroom.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 选课接口 MockMvc 集成测试：验证 enrollment:read / enrollment:write 的方法级授权。
 * Service 与 RBAC Mapper 用 Mock 隔离，不依赖 MySQL/Redis。
 */
@WebMvcTest(controllers = EnrollmentController.class)
@ContextConfiguration(classes = {
        EnrollmentController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtUtil.class
})
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private EnrollmentService enrollmentService;

    @MockBean
    private PermissionMapper permissionMapper;

    @MockBean
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        // 学生(userId=2)有选课/退课 + 查看权限；教师(userId=1)仅查看权限
        when(permissionMapper.selectPermCodesByUserId(1L))
                .thenReturn(List.of("enrollment:read"));
        when(permissionMapper.selectPermCodesByUserId(2L))
                .thenReturn(List.of("enrollment:read", "enrollment:write"));
        User active = new User();
        active.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(active);
        when(userMapper.selectById(2L)).thenReturn(active);
    }

    private String bearer(Long userId, String username, String role) {
        return "Bearer " + jwtUtil.generateToken(userId, username, role);
    }

    @Test
    void studentCanEnroll() throws Exception {
        mockMvc.perform(post("/api/enrollments/10")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void teacherWithoutWriteAuthorityGets403OnEnroll() throws Exception {
        mockMvc.perform(post("/api/enrollments/10")
                        .header("Authorization", bearer(1L, "teacher", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void studentCanViewMyCourses() throws Exception {
        mockMvc.perform(get("/api/enrollments/my")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void teacherCanViewCourseStudents() throws Exception {
        mockMvc.perform(get("/api/enrollments/course/10/students")
                        .header("Authorization", bearer(1L, "teacher", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void studentCanUnenroll() throws Exception {
        mockMvc.perform(delete("/api/enrollments/10")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
