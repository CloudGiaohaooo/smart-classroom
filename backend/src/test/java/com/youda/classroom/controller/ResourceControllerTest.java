package com.youda.classroom.controller;

import com.youda.classroom.cache.PermissionCacheService;
import com.youda.classroom.config.SecurityConfig;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.PermissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.JwtAuthenticationFilter;
import com.youda.classroom.security.JwtUtil;
import com.youda.classroom.service.ResourceService;
import com.youda.classroom.vo.ResourceUploadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 课程资源接口 MockMvc 集成测试（创新点 C 配套）。
 * 只加载控制器 + 真实 Security 过滤链 + 真实 JWT 工具，Service 与 RBAC 权限 Mapper 用 Mock 隔离，
 * 因此不依赖 MySQL/Redis，可在 CI 与无外部服务的环境直接跑。
 *
 * 覆盖：
 * 1. 未登录 → HTTP 恒 200、响应体业务码 401（兼容前端统一拦截的既定设计）；
 * 2. 已登录但无写权限 → 业务码 403（@PreAuthorize 方法级授权）；
 * 3. 有权限 → 200 + 秒传去重响应字段。
 */
@WebMvcTest(controllers = ResourceController.class)
@ContextConfiguration(classes = {
        ResourceController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtUtil.class
})
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private ResourceService resourceService;

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
        // 教师(userId=1)拥有读+写权限；学生(userId=2)仅读权限
        when(permissionMapper.selectPermCodesByUserId(1L))
                .thenReturn(List.of("resource:read", "resource:write"));
        when(permissionMapper.selectPermCodesByUserId(2L))
                .thenReturn(List.of("resource:read"));
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
    void unauthenticatedRequestReturns401BodyCode() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())               // 项目约定：HTTP 恒 200
                .andExpect(jsonPath("$.code").value(401)); // 业务码 401 = 未登录
    }

    @Test
    void teacherWithReadAuthorityCanQueryResources() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", bearer(1L, "teacher", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void studentWithoutWriteAuthorityGets403OnUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", "content".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .param("courseId", "1")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void teacherUploadReturnsDeduplicatedFlag() throws Exception {
        ResourceUploadVO vo = new ResourceUploadVO();
        vo.setResourceId(100L);
        vo.setFileMd5("d41d8cd98f00b204e9800998ecf8427e");
        vo.setDeduplicated(true);
        vo.setFileUrl("/files/1/abc.pdf");
        when(resourceService.upload(any(), any(), any())).thenReturn(vo);

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", "content".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .param("courseId", "1")
                        .header("Authorization", bearer(1L, "teacher", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("秒传成功：相同文件已存在，已复用"))
                .andExpect(jsonPath("$.data.deduplicated").value(true))
                .andExpect(jsonPath("$.data.fileMd5").value("d41d8cd98f00b204e9800998ecf8427e"));
    }
}
