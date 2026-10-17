package com.youda.classroom.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.SysRole;
import com.youda.classroom.entity.SysUserRole;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.RoleMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 雷同检测演示数据初始化器（Phase 3 创新点 A）：
 * 幂等地创建一个演示作业 + 20 名演示学生 + 20 条「成簇」的提交，
 * 用于在批阅页直观展示 TF-IDF 聚类效果：同簇提交相似度 >90%（标记疑似雷同），跨簇相似度低。
 *
 * 20 条提交按 5 簇组织（每簇 4 人），簇内文本几乎一致（仅结尾略有差异），簇间主题不同。
 * 其中前 9 条已批阅并使用了 3 种评语（各重复 3 次），用于演示「评语推荐」的频率统计。
 *
 * @Order(3)：在 DataInitializer(1)、RbacInitializer(2) 之后执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class SimilarityDemoDataInitializer implements CommandLineRunner {

    private static final String DEMO_ASSIGNMENT_TITLE = "实验二：数组与函数（雷同检测演示）";

    /** 5 簇基准文本（簇内几乎相同，簇间主题不同） */
    private static final String[] CLUSTER_BASE = {
            "本程序使用冒泡排序对整型数组进行升序排列，将排序逻辑封装在 sort 函数中，主函数负责输入输出与调用。时间复杂度为 O(n^2)，空间复杂度为 O(1)。",
            "本程序使用选择排序对整型数组进行升序排列，每一轮选出未排序部分的最小值放到前面，排序逻辑封装在 sort 函数中。时间复杂度为 O(n^2)。",
            "本程序使用快速排序对整型数组进行升序排列，采用分治思想，选取基准值后递归划分左右区间，封装在 quickSort 函数中。平均时间复杂度 O(n log n)。",
            "本程序使用插入排序对整型数组进行升序排列，将元素逐个插入到已排序的合适位置，封装在 insertSort 函数中。时间复杂度 O(n^2)。",
            "本程序实现了数组排序功能，并对冒泡、选择、快速三种排序算法进行了性能对比分析，附上了运行结果截图与代码说明。"
    };

    /** 簇内差异（仅结尾措辞不同，制造高相似度） */
    private static final String[] TAILS = {"", " 运行结果正确。", " 已通过编译与测试。", " 代码注释完整。"};

    /** 用于演示评语推荐的 3 种评语（各重复 3 次，共 9 条已批阅提交） */
    private static final String[] FEEDBACKS = {
            "排序思路正确，注意边界条件。",
            "代码规范，逻辑清晰，继续保持。",
            "基本正确，建议补充时间复杂度分析。"
    };

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (assignmentMapper.selectOne(new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getTitle, DEMO_ASSIGNMENT_TITLE)) != null) {
            return; // 已存在，幂等跳过
        }
        Course course = courseMapper.selectList(null).stream().findFirst().orElse(null);
        if (course == null) {
            log.warn("未找到课程，跳过雷同检测演示数据初始化");
            return;
        }
        log.info("初始化雷同检测演示数据（20 名学生 / 1 作业 / 20 条成簇提交）...");

        // 1. 演示作业
        Assignment assignment = new Assignment();
        assignment.setCourseId(course.getId());
        assignment.setTitle(DEMO_ASSIGNMENT_TITLE);
        assignment.setContent("实现数组排序并封装成函数，提交源码与思路说明。");
        assignment.setDeadline(LocalDateTime.now().plusDays(14));
        assignment.setTotalScore(new BigDecimal("100"));
        assignmentMapper.insert(assignment);

        // 2. 20 名演示学生（绑定 STUDENT 角色，可登录 demo01~demo20 / 123456）
        Long studentRoleId = resolveStudentRoleId();
        List<Long> studentIds = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User u = newUser("demo" + String.format("%02d", i), "演示学生" + String.format("%02d", i));
            userMapper.insert(u);
            studentIds.add(u.getId());
            if (studentRoleId != null) {
                bindStudentRole(u.getId(), studentRoleId);
            }
        }

        // 3. 20 条成簇提交（每簇 4 人）；前 9 条已批阅（用于评语推荐）
        for (int i = 0; i < 20; i++) {
            Submission s = new Submission();
            s.setAssignmentId(assignment.getId());
            s.setStudentId(studentIds.get(i));
            s.setContent(buildContent(i));
            s.setSubmitTime(LocalDateTime.now());
            if (i < 9) {
                String feedback = FEEDBACKS[i / 3]; // 每 3 条同一评语
                s.setScore(new BigDecimal(String.valueOf(80 + (i % 3) * 5)));
                s.setFeedback(feedback);
                s.setStatus("GRADED");
                s.setGradeTime(LocalDateTime.now());
            } else {
                s.setStatus("SUBMITTED");
            }
            submissionMapper.insert(s);
        }
        log.info("雷同检测演示数据初始化完成，作业 ID={}", assignment.getId());
    }

    private String buildContent(int idx) {
        return CLUSTER_BASE[idx / 4] + TAILS[idx % 4];
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
        if (count == 0) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private User newUser(String username, String realName) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setRole("STUDENT");
        u.setStatus(1);
        return u;
    }
}
