package com.youda.classroom.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.CourseEnrollment;
import com.youda.classroom.entity.StudentBehavior;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.SysRole;
import com.youda.classroom.entity.SysUserRole;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseEnrollmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.RoleMapper;
import com.youda.classroom.mapper.StudentBehaviorMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 模拟演示数据生成器（默认关闭，`demo.simulated.enabled=true` 时启用）。
 *
 * <p>用<b>固定种子的随机数</b>（{@link Random}）生成一个「更接近真实班级」的模拟数据集，
 * 让学习预警看板 / 提交率趋势 / 资源热力在答辩现场呈现真实感的活跃-沉默分布。</p>
 *
 * <p><b>诚实边界（重要）</b>：这里生成的是<b>模拟数据</b>，不是「真实运行数据」。
 * 真实采集仍需按 docs/evaluation/真实数据采集步骤.md 找同学真实使用后采集，二者在答辩材料中
 * 必须分开陈述——不得把本模拟数据描述成真实采集结果。</p>
 *
 * <p>生成内容（种子 {@value #RANDOM_SEED} 固定，可复现）：</p>
 * <ul>
 *   <li>{@value #STUDENT_COUNT} 名模拟学生 sim01~sim40（密码 123456，可登录，绑定 STUDENT 角色）；</li>
 *   <li>选课：每人 1~3 门（从已有课程中随机）；</li>
 *   <li>提交：活跃档约 90% / 一般档约 65% / 沉默档约 20%（且只出现在 8 天前），
 *       得分近似 N(82,9) 截断到 [55,100]，约 85% 已批阅；</li>
 *   <li>行为埋点：近 14 天，分活跃（约 55%）/ 一般（约 30%）/ 沉默（约 15%）三档，
 *       沉默档近 7 天无登录，用于触发「长期未登录」预警。</li>
 * </ul>
 *
 * @Order(5)：在 DataInitializer(1) / RbacInitializer(2) / SimilarityDemoDataInitializer(3) /
 * EnrollmentInitializer(4) 之后执行，复用它们建好的课程与作业。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.simulated.enabled", havingValue = "true")
@Order(5)
public class SimulatedDataInitializer implements CommandLineRunner {

    private static final int STUDENT_COUNT = 40;
    private static final long RANDOM_SEED = 20261019L;
    private static final String SIM_USERNAME_PREFIX = "sim";

    /** 活跃度档位：0=活跃 / 1=一般 / 2=沉默 */
    private static final int TIER_ACTIVE = 0;
    private static final int TIER_MODERATE = 1;
    private static final int TIER_SILENT = 2;

    private static final String[] SUBMIT_CONTENTS = {
            "已完成本次作业，源码与运行截图见附件。",
            "作业已提交，实现思路写在注释里。",
            "按要求完成，本地测试用例全部通过。",
            "已提交，关键部分附了说明与截图。"
    };
    private static final String[] FEEDBACKS = {
            "完成情况良好，思路清晰。",
            "基本正确，注意边界条件。",
            "符合要求，可以再补充一点说明。"
    };

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final StudentBehaviorMapper behaviorMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        List<Course> courses = courseMapper.selectList(null);
        if (courses.isEmpty()) {
            log.warn("缺少课程，跳过模拟数据生成");
            return;
        }
        Long existing = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .likeRight(User::getUsername, SIM_USERNAME_PREFIX));
        if (existing != null && existing > 0) {
            log.info("模拟数据已存在（{} 名模拟学生），跳过生成", existing);
            return;
        }

        log.info("生成模拟演示数据（{} 名学生，随机种子 {}）...", STUDENT_COUNT, RANDOM_SEED);
        Random rnd = new Random(RANDOM_SEED);
        LocalDateTime now = LocalDateTime.now();
        Long studentRoleId = resolveStudentRoleId();

        // 每门课的作业，供提交与行为埋点引用
        Map<Long, List<Assignment>> assignmentsByCourse = new HashMap<>();
        for (Course c : courses) {
            assignmentsByCourse.put(c.getId(), assignmentMapper.selectList(
                    new LambdaQueryWrapper<Assignment>().eq(Assignment::getCourseId, c.getId())));
        }

        // 1. 学生 + RBAC 角色；同时定档活跃度与选课
        List<User> students = new ArrayList<>();
        Map<Long, Integer> tier = new HashMap<>();
        Map<Long, List<Course>> enrolled = new HashMap<>();
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            User u = new User();
            u.setUsername(String.format("sim%02d", i));
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRealName("模拟学生" + String.format("%02d", i));
            u.setRole("STUDENT");
            u.setStatus(1);
            userMapper.insert(u);
            students.add(u);
            if (studentRoleId != null) {
                bindStudentRole(u.getId(), studentRoleId);
            }
            tier.put(u.getId(), pickTier(rnd));

            // 选课：1~3 门
            int courseCount = 1 + rnd.nextInt(3);
            List<Course> picked = new ArrayList<>();
            while (picked.size() < courseCount && picked.size() < courses.size()) {
                Course c = courses.get(rnd.nextInt(courses.size()));
                if (!picked.contains(c)) {
                    picked.add(c);
                }
            }
            enrolled.put(u.getId(), picked);
            for (Course c : picked) {
                CourseEnrollment ce = new CourseEnrollment();
                ce.setCourseId(c.getId());
                ce.setStudentId(u.getId());
                ce.setEnrollTime(now.minusDays(1 + rnd.nextInt(14)));
                enrollmentMapper.insert(ce);
            }
        }

        // 2. 提交 + 3. 行为埋点（按活跃度档位保持两者一致）
        int gradedCount = 0;
        int behaviorCount = 0;
        for (User s : students) {
            int t = tier.get(s.getId());
            List<Course> myCourses = enrolled.get(s.getId());

            // 提交：按档位决定完成率；沉默档只提交 8 天前（制造「近 7 天未交」）
            for (Course c : myCourses) {
                List<Assignment> assignments = assignmentsByCourse.getOrDefault(c.getId(), List.of());
                for (Assignment a : assignments) {
                    if (rnd.nextDouble() < completionRate(t)) {
                        int daysAgo = t == TIER_SILENT ? 8 + rnd.nextInt(6) : rnd.nextInt(7);
                        LocalDateTime submitTime = now.minusDays(daysAgo)
                                .minusHours(rnd.nextInt(6)).minusMinutes(rnd.nextInt(60));
                        if (insertSubmission(s.getId(), a, submitTime, rnd)) {
                            gradedCount++;
                        }
                    }
                }
            }

            // 行为埋点：近 14 天
            for (int d = 13; d >= 0; d--) {
                if (t == TIER_SILENT) {
                    // 沉默档只在 8 天前偶有登录，近 7 天无登录 → 触发「长期未登录」预警
                    if (d >= 8 && rnd.nextDouble() < 0.5) {
                        insertBehavior(s.getId(), "LOGIN", null, timeAt(rnd, now, d));
                        behaviorCount++;
                    }
                    continue;
                }
                double loginP = t == TIER_ACTIVE ? 0.8 : 0.45;
                double viewP = t == TIER_ACTIVE ? 0.6 : 0.3;
                double submitP = t == TIER_ACTIVE ? 0.15 : 0.08;
                if (rnd.nextDouble() < loginP) {
                    insertBehavior(s.getId(), "LOGIN", null, timeAt(rnd, now, d));
                    behaviorCount++;
                }
                if (!myCourses.isEmpty() && rnd.nextDouble() < viewP) {
                    Course c = myCourses.get(rnd.nextInt(myCourses.size()));
                    insertBehavior(s.getId(), "VIEW_RESOURCE", c.getId(), timeAt(rnd, now, d));
                    behaviorCount++;
                }
                if (!myCourses.isEmpty() && rnd.nextDouble() < submitP) {
                    Course c = myCourses.get(rnd.nextInt(myCourses.size()));
                    List<Assignment> as = assignmentsByCourse.getOrDefault(c.getId(), List.of());
                    Long aid = as.isEmpty() ? null : as.get(rnd.nextInt(as.size())).getId();
                    insertBehavior(s.getId(), "SUBMIT_ASSIGNMENT", aid, timeAt(rnd, now, d));
                    behaviorCount++;
                }
            }
        }

        log.info("模拟演示数据生成完成：{} 名学生 / 已批阅提交 {} 条 / 行为埋点 {} 条（模拟数据，非真实采集）",
                students.size(), gradedCount, behaviorCount);
    }

    private int pickTier(Random rnd) {
        double p = rnd.nextDouble();
        if (p < 0.55) {
            return TIER_ACTIVE;
        }
        if (p < 0.85) {
            return TIER_MODERATE;
        }
        return TIER_SILENT;
    }

    private double completionRate(int tier) {
        return switch (tier) {
            case TIER_ACTIVE -> 0.90;
            case TIER_MODERATE -> 0.65;
            default -> 0.20;
        };
    }

    private boolean submissionGraded(Random rnd) {
        return rnd.nextDouble() < 0.85;
    }

    private LocalDateTime timeAt(Random rnd, LocalDateTime now, int daysAgo) {
        return now.minusDays(daysAgo).minusHours(1 + rnd.nextInt(8)).minusMinutes(rnd.nextInt(60));
    }

    private boolean insertSubmission(Long studentId, Assignment a, LocalDateTime submitTime, Random rnd) {
        Submission s = new Submission();
        s.setAssignmentId(a.getId());
        s.setStudentId(studentId);
        s.setContent(SUBMIT_CONTENTS[rnd.nextInt(SUBMIT_CONTENTS.length)]);
        s.setSubmitTime(submitTime);
        boolean graded = submissionGraded(rnd);
        if (graded) {
            int score = clamp((int) Math.round(rnd.nextGaussian() * 9 + 82), 55, 100);
            s.setScore(new BigDecimal(score));
            s.setFeedback(FEEDBACKS[rnd.nextInt(FEEDBACKS.length)]);
            s.setStatus("GRADED");
            s.setGradeTime(submitTime.plusHours(1 + rnd.nextInt(48)));
        } else {
            s.setStatus("SUBMITTED");
        }
        submissionMapper.insert(s);
        return graded;
    }

    private void insertBehavior(Long studentId, String type, Long targetId, LocalDateTime occurTime) {
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

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private Long resolveStudentRoleId() {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "STUDENT"));
        return role == null ? null : role.getId();
    }

    private void bindStudentRole(Long userId, Long roleId) {
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
        if (count == null || count == 0) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
