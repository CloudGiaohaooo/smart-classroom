package com.youda.classroom.controller;

import com.youda.classroom.cache.PermissionCacheService;
import com.youda.classroom.config.SecurityConfig;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.PermissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.JwtAuthenticationFilter;
import com.youda.classroom.security.JwtUtil;
import com.youda.classroom.service.GradingAssistService;
import com.youda.classroom.service.SubmissionService;
import com.youda.classroom.vo.FeedbackSuggestionVO;
import com.youda.classroom.vo.GradingAssistVO;
import com.youda.classroom.vo.SimilarSubmissionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 提交 / 批阅接口 MockMvc 集成测试（创新点 A 配套）。
 * 用真实 Security 过滤链 + 真实 JWT 验证「批阅辅助」「批阅」「提交」三类接口的方法级授权。
 * Service 与 RBAC Mapper 用 Mock 隔离，不依赖 MySQL/Redis。
 */
@WebMvcTest(controllers = SubmissionController.class)
@ContextConfiguration(classes = {
        SubmissionController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtUtil.class
})
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private GradingAssistService gradingAssistService;

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
        // 教师(userId=1)有批阅/读取/提交权限；学生(userId=2)只能读取与提交，无批阅权限
        when(permissionMapper.selectPermCodesByUserId(1L))
                .thenReturn(List.of("submission:read", "submission:submit", "submission:grade"));
        when(permissionMapper.selectPermCodesByUserId(2L))
                .thenReturn(List.of("submission:read", "submission:submit"));
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
    void studentWithoutGradeAuthorityGets403OnGradingAssist() throws Exception {
        mockMvc.perform(get("/api/submissions/1/grading-assist")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void teacherGetsGradingAssistWithSimilarAndSuggestions() throws Exception {
        SimilarSubmissionVO sim = new SimilarSubmissionVO();
        sim.setSubmissionId(5L);
        sim.setStudentId(20L);
        sim.setStudentName("李四");
        sim.setSimilarity(93.7);
        sim.setFlagged(true);
        sim.setExcerpt("本程序使用冒泡排序对整型数组进行升序排列...");

        FeedbackSuggestionVO sug = new FeedbackSuggestionVO();
        sug.setText("思路清晰，注释完整");
        sug.setCount(3L);
        sug.setAvgScore(BigDecimal.valueOf(85.0));

        when(gradingAssistService.assist(1L))
                .thenReturn(new GradingAssistVO(List.of(sim), List.of(sug)));

        mockMvc.perform(get("/api/submissions/1/grading-assist")
                        .header("Authorization", bearer(1L, "teacher", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.similar[0].similarity").value(93.7))
                .andExpect(jsonPath("$.data.similar[0].flagged").value(true))
                .andExpect(jsonPath("$.data.suggestions[0].text").value("思路清晰，注释完整"));
    }

    @Test
    void studentWithoutGradeAuthorityGets403OnGrade() throws Exception {
        mockMvc.perform(put("/api/submissions/1/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":90,\"feedback\":\"不错\"}")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void studentWithSubmitAuthorityCanSubmit() throws Exception {
        mockMvc.perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":1,\"content\":\"实验一提交\"}")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("提交成功"));
    }

    @Test
    void studentQueryingPageIsForcedToOwnStudentId() throws Exception {
        // P0-A：学生查询提交列表时，无论是否传 studentId，都被强制为本人 id=2（防止读全站）
        mockMvc.perform(get("/api/submissions")
                        .param("size", "100")
                        .header("Authorization", bearer(2L, "student", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(submissionService).page(anyLong(), anyLong(), isNull(), eq(2L), isNull());
    }
}
