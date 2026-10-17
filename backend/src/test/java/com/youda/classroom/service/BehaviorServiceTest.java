package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.StudentBehaviorMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.vo.BehaviorDashboardVO;
import com.youda.classroom.vo.BehaviorLastVO;
import com.youda.classroom.vo.StudentWarningVO;
import com.youda.classroom.vo.SubmitTrendPointVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 学习预警服务单测（创新点 B）：
 * 纯 Mock 依赖，验证「预警规则阈值判断」与「趋势补零」两个核心逻辑。
 */
class BehaviorServiceTest {

    private BehaviorService service;
    private StudentBehaviorMapper behaviorMapper;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        behaviorMapper = mock(StudentBehaviorMapper.class);
        userMapper = mock(UserMapper.class);
        service = new BehaviorService(behaviorMapper, userMapper);
        // @Value 字段在纯单测中不会自动注入，手动赋值模拟 application.yml 默认值
        ReflectionTestUtils.setField(service, "loginInactiveDays", 7);
        ReflectionTestUtils.setField(service, "noSubmitDays", 7);
        ReflectionTestUtils.setField(service, "trendDays", 7);
        ReflectionTestUtils.setField(service, "heatLimit", 10);
    }

    @Test
    void warningFlagsOnlyInactiveStudent() {
        User active = new User();
        active.setId(1L);
        active.setRealName("活跃学生");
        User inactive = new User();
        inactive.setId(2L);
        inactive.setRealName("静默学生");
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(active, inactive));

        // 活跃学生近期有登录+提交；静默学生 30 天前登录、无提交
        BehaviorLastVO aLogin = last(1L, "LOGIN", LocalDateTime.now());
        BehaviorLastVO aSubmit = last(1L, "SUBMIT_ASSIGNMENT", LocalDateTime.now());
        BehaviorLastVO bLogin = last(2L, "LOGIN", LocalDateTime.now().minusDays(30));
        when(behaviorMapper.selectLastTimePerType()).thenReturn(List.of(aLogin, aSubmit, bLogin));
        when(behaviorMapper.selectSubmitTrend(anyInt())).thenReturn(List.of());
        when(behaviorMapper.selectResourceHeat(anyInt())).thenReturn(List.of());

        BehaviorDashboardVO vo = service.dashboard();

        List<StudentWarningVO> warnings = vo.getWarningList();
        assertEquals(1, warnings.size(), "只有静默学生应被预警");
        assertEquals(2L, warnings.get(0).getStudentId());
        assertTrue(warnings.get(0).getReason().contains("长期未登录"));
        assertTrue(warnings.get(0).getReason().contains("长期未提交作业"));
    }

    @Test
    void submitTrendFillsMissingDaysWithZero() {
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(behaviorMapper.selectLastTimePerType()).thenReturn(List.of());
        when(behaviorMapper.selectResourceHeat(anyInt())).thenReturn(List.of());
        // 仅今天有 5 次提交
        SubmitTrendPointVO point = new SubmitTrendPointVO(LocalDate.now().toString(), 5L);
        when(behaviorMapper.selectSubmitTrend(7)).thenReturn(List.of(point));

        List<SubmitTrendPointVO> trend = service.dashboard().getSubmitTrend();

        assertEquals(7, trend.size(), "应补齐到 7 天");
        long total = trend.stream().mapToLong(SubmitTrendPointVO::getCount).sum();
        assertEquals(5L, total, "总提交量应与埋点一致");
    }

    private BehaviorLastVO last(Long studentId, String type, LocalDateTime time) {
        BehaviorLastVO vo = new BehaviorLastVO();
        vo.setStudentId(studentId);
        vo.setBehaviorType(type);
        vo.setLastTime(time);
        return vo;
    }
}
