package com.youda.classroom.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.StudentBehavior;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.StudentBehaviorMapper;
import com.youda.classroom.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习行为埋点演示数据初始化器（Phase 4 创新点 B）：
 * 幂等地为全部学生种入近两周的 LOGIN / VIEW_RESOURCE / SUBMIT_ASSIGNMENT 埋点，
 * 其中 3 名演示学生（demo18~demo20）只保留 30 天前的旧行为，
 * 用于在预警看板中触发「长期未登录 / 长期未提交作业」预警。
 *
 * @Order(4)：在 DataInitializer(1)、RbacInitializer(2)、SimilarityDemoDataInitializer(3) 之后执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class BehaviorDemoDataInitializer implements CommandLineRunner {

    /** 故意保持「不活跃」的学生，用于触发预警演示 */
    private static final List<String> INACTIVE_USERNAMES = List.of("demo18", "demo19", "demo20");

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final StudentBehaviorMapper behaviorMapper;

    @Override
    public void run(String... args) {
        if (behaviorMapper.selectCount(null) > 0) {
            return; // 已有埋点数据，幂等跳过
        }
        List<User> students = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT"));
        List<Course> courses = courseMapper.selectList(null);
        List<Assignment> assignments = assignmentMapper.selectList(null);
        if (students.isEmpty() || courses.isEmpty()) {
            log.warn("缺少学生或课程，跳过学习行为埋点演示数据初始化");
            return;
        }
        log.info("初始化学习行为埋点演示数据（{} 名学生 / {} 门课程）...", students.size(), courses.size());

        LocalDateTime now = LocalDateTime.now();
        int idx = 0;
        for (User student : students) {
            if (INACTIVE_USERNAMES.contains(student.getUsername())) {
                // 只保留 30 天前的旧行为，触发预警
                insert(student.getId(), "LOGIN", null, now.minusDays(30));
                insert(student.getId(), "VIEW_RESOURCE",
                        courses.get(idx % courses.size()).getId(), now.minusDays(30));
                idx++;
                continue;
            }
            // 近 7 天的活跃行为：登录 / 看资源 / 交作业（保证时间都在过去）
            for (int d = 6; d >= 0; d--) {
                Long courseId = courses.get((idx + d) % courses.size()).getId();
                insert(student.getId(), "LOGIN", null, now.minusDays(d).minusHours(5).minusMinutes(idx));
                insert(student.getId(), "VIEW_RESOURCE", courseId, now.minusDays(d).minusHours(3).minusMinutes(idx));
                if (d % 2 == 0) {
                    Long assignmentId = assignments.isEmpty()
                            ? null : assignments.get(idx % assignments.size()).getId();
                    insert(student.getId(), "SUBMIT_ASSIGNMENT", assignmentId,
                            now.minusDays(d).minusHours(1).minusMinutes(idx));
                }
            }
            idx++;
        }
        log.info("学习行为埋点演示数据初始化完成");
    }

    private void insert(Long studentId, String type, Long targetId, LocalDateTime occurTime) {
        StudentBehavior b = new StudentBehavior();
        b.setStudentId(studentId);
        b.setBehaviorType(type);
        b.setTargetId(targetId);
        b.setDetail(switch (type) {
            case "LOGIN" -> "登录系统";
            case "VIEW_RESOURCE" -> "浏览课程资源";
            case "SUBMIT_ASSIGNMENT" -> "提交作业";
            default -> type;
        });
        b.setOccurTime(occurTime);
        behaviorMapper.insert(b);
    }
}
