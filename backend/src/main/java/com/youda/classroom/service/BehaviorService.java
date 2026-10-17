package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.StudentBehaviorMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.vo.BehaviorDashboardVO;
import com.youda.classroom.vo.BehaviorLastVO;
import com.youda.classroom.vo.ResourceHeatVO;
import com.youda.classroom.vo.StudentWarningVO;
import com.youda.classroom.vo.SubmitTrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习行为看板服务（Phase 4 创新点 B）：
 * 提交率趋势 + 资源访问热力 + 学习预警列表，全部来源于 AOP 埋点表 student_behavior。
 * 预警规则阈值通过 application.yml 外置，避免硬编码魔法值。
 */
@Service
@RequiredArgsConstructor
public class BehaviorService {

    private final StudentBehaviorMapper behaviorMapper;
    private final UserMapper userMapper;

    @Value("${warning.login-inactive-days}")
    private int loginInactiveDays;

    @Value("${warning.no-submit-days}")
    private int noSubmitDays;

    @Value("${warning.trend-days}")
    private int trendDays;

    @Value("${warning.heat-limit}")
    private int heatLimit;

    public BehaviorDashboardVO dashboard() {
        BehaviorDashboardVO vo = new BehaviorDashboardVO();
        vo.setSubmitTrend(buildSubmitTrend());
        vo.setResourceHeat(behaviorMapper.selectResourceHeat(heatLimit));
        vo.setWarningList(buildWarnings());
        return vo;
    }

    /** 近 N 天提交量趋势：补齐无提交的日期为 0，保证图表连续 */
    private List<SubmitTrendPointVO> buildSubmitTrend() {
        Map<String, Long> byDate = new HashMap<>();
        for (SubmitTrendPointVO r : behaviorMapper.selectSubmitTrend(trendDays)) {
            byDate.put(r.getDate(), r.getCount());
        }
        List<SubmitTrendPointVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = trendDays - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            result.add(new SubmitTrendPointVO(date, byDate.getOrDefault(date, 0L)));
        }
        return result;
    }

    /** 学习预警：长期未登录 / 长期未提交作业，阈值来自 application.yml */
    private List<StudentWarningVO> buildWarnings() {
        List<User> students = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT"));
        Map<Long, LocalDateTime> lastLogin = new HashMap<>();
        Map<Long, LocalDateTime> lastSubmit = new HashMap<>();
        for (BehaviorLastVO l : behaviorMapper.selectLastTimePerType()) {
            if ("LOGIN".equals(l.getBehaviorType())) {
                lastLogin.put(l.getStudentId(), l.getLastTime());
            } else if ("SUBMIT_ASSIGNMENT".equals(l.getBehaviorType())) {
                lastSubmit.put(l.getStudentId(), l.getLastTime());
            }
        }

        LocalDate today = LocalDate.now();
        List<StudentWarningVO> warnings = new ArrayList<>();
        for (User s : students) {
            LocalDateTime login = lastLogin.get(s.getId());
            LocalDateTime submit = lastSubmit.get(s.getId());
            List<String> reasons = new ArrayList<>();
            if (isInactive(login, loginInactiveDays)) {
                reasons.add("长期未登录");
            }
            if (isInactive(submit, noSubmitDays)) {
                reasons.add("长期未提交作业");
            }
            if (reasons.isEmpty()) {
                continue;
            }

            LocalDateTime lastActive = later(login, submit);
            Long days = lastActive == null
                    ? null
                    : ChronoUnit.DAYS.between(lastActive.toLocalDate(), today);
            warnings.add(new StudentWarningVO(s.getId(), s.getRealName(),
                    String.join("、", reasons), lastActive, days));
        }
        // 按静默天数降序：最不活跃的学生排在前面
        warnings.sort((a, b) -> {
            if (a.getInactiveDays() == null && b.getInactiveDays() == null) {
                return 0;
            }
            if (a.getInactiveDays() == null) {
                return -1;
            }
            if (b.getInactiveDays() == null) {
                return 1;
            }
            return Long.compare(b.getInactiveDays(), a.getInactiveDays());
        });
        return warnings;
    }

    private boolean isInactive(LocalDateTime last, int days) {
        return last == null || last.isBefore(LocalDateTime.now().minusDays(days));
    }

    private LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }
}
