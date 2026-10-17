package com.youda.classroom.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.annotation.BehaviorLog;
import com.youda.classroom.dto.LoginDTO;
import com.youda.classroom.dto.SubmitDTO;
import com.youda.classroom.entity.StudentBehavior;
import com.youda.classroom.entity.User;
import com.youda.classroom.enums.BehaviorType;
import com.youda.classroom.mapper.StudentBehaviorMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 学习行为埋点切面（Phase 4 创新点 B）：
 * 通过 {@code @BehaviorLog} 注解声明式埋点，与业务代码完全解耦。
 * 仅记录「学生」角色的行为；埋点为旁路能力，任何异常只告警、绝不打断业务主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BehaviorLogAspect {

    private final StudentBehaviorMapper behaviorMapper;
    private final UserMapper userMapper;

    @AfterReturning(pointcut = "@annotation(behaviorLog)")
    public void record(JoinPoint joinPoint, BehaviorLog behaviorLog) {
        try {
            BehaviorType type = behaviorLog.value();
            Long studentId = resolveStudentId(joinPoint, type);
            if (studentId == null) {
                return; // 非学生行为，不记录
            }
            StudentBehavior b = new StudentBehavior();
            b.setStudentId(studentId);
            b.setBehaviorType(type.name());
            b.setTargetId(resolveTargetId(joinPoint, type));
            b.setDetail(resolveDetail(type));
            b.setOccurTime(LocalDateTime.now());
            behaviorMapper.insert(b);
        } catch (Exception e) {
            log.warn("学习行为埋点失败（不影响业务）: {}", e.getMessage());
        }
    }

    /**
     * 解析行为主体（学生 id）：
     * 登录时认证上下文尚未建立，从 LoginDTO 反查用户；其余事件直接取 ThreadLocal 上下文。
     * 非学生角色返回 null，跳过埋点。
     */
    private Long resolveStudentId(JoinPoint joinPoint, BehaviorType type) {
        if (type == BehaviorType.LOGIN) {
            LoginDTO dto = argOf(joinPoint, LoginDTO.class);
            if (dto == null) {
                return null;
            }
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
            return (user != null && "STUDENT".equals(user.getRole())) ? user.getId() : null;
        }
        if (!"STUDENT".equals(UserContext.getRole())) {
            return null;
        }
        return UserContext.getUserId();
    }

    private Long resolveTargetId(JoinPoint joinPoint, BehaviorType type) {
        if (type == BehaviorType.SUBMIT_ASSIGNMENT) {
            SubmitDTO dto = argOf(joinPoint, SubmitDTO.class);
            return dto == null ? null : dto.getAssignmentId();
        }
        if (type == BehaviorType.VIEW_RESOURCE) {
            // 浏览资源列表时，目标为课程 id（由请求参数 courseId 传入）
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs == null ? null : attrs.getRequest();
            String courseId = request == null ? null : request.getParameter("courseId");
            return (courseId == null || courseId.isBlank()) ? null : Long.valueOf(courseId);
        }
        return null;
    }

    private String resolveDetail(BehaviorType type) {
        return switch (type) {
            case LOGIN -> "登录系统";
            case VIEW_RESOURCE -> "浏览课程资源";
            case SUBMIT_ASSIGNMENT -> "提交作业";
        };
    }

    private <T> T argOf(JoinPoint joinPoint, Class<T> clazz) {
        for (Object arg : joinPoint.getArgs()) {
            if (clazz.isInstance(arg)) {
                return clazz.cast(arg);
            }
        }
        return null;
    }
}
