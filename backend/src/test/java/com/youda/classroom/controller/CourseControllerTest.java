package com.youda.classroom.controller;

import com.youda.classroom.cache.PermissionCacheService;
import com.youda.classroom.config.SecurityConfig;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.PermissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.JwtAuthenticationFilter;
import com.youda.classroom.security.JwtUtil;
import com.youda.classroom.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 课程管理接口 MockMvc 集成测试：验证 RBAC 方法级授权（course:read / course:write）。
 * Service 与 RBAC Mapper 用 Mock 隔离，不依赖 MySQL/Redis。
 */
@WebMvcTest(controllers = CourseController.class)
@ContextConfiguration(classes = {
        CourseController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtUtil.class
})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private CourseService courseService;

    @MockBean
    private PermissionMapper permissionMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private PermissionCacheService permissionCache;

    @BeforeEach
    void setUp() {
        // 权限缓存初始为空（未命中返回 null），JWT 过滤器回源 DB 加载并回填
        when(permissionCache.get(any())).thenReturn(null);
        // 教师(userId=1)有读+写权限；学生(userId=2)仅读权限
        when(permissionMapper.selectPermCodesByUserId(1L))
                .thenReturn(List.of("course:read", "course:write"));
        when(permissionMapper.selectPermCodesByUserId(2L))
                .thenReturn(List.of("course:read"));
        // JWT 过滤器会复查账号状态：返回未禁用的账号，保证认证通过
        User active = new User();
        active.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(active);
        when(userMapper.selectById(2L)).thenReturn(active);
    }

    private String bearer(Long userId, String username, String role) {
        return "Bearer " + jwtUtil.generateToken(userId, username, role);
    }

    @Test
    void studentWithoutWriteAuthorityGets403OnCreate() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"数据结构\",\"teacherId\":1}")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void teacherWithWriteAuthorityCanCreateCourse() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"数据结构\",\"teacherId\":1}")
                        .header("Authorization", bearer(1L, "teacher", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void studentWithReadAuthorityCanQueryCourses() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
