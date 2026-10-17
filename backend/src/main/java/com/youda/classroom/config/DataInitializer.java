package com.youda.classroom.config;

import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 演示数据初始化器：
 * 首次启动（用户表为空）时自动写入演示账号与样例数据，方便答辩现场直接演示。
 * 所有演示账号密码均为 123456。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        log.info("首次启动，正在初始化演示数据...");

        // 1. 用户
        User admin = newUser("admin", "系统管理员", "ADMIN");
        User teacher = newUser("teacher", "王老师", "TEACHER");
        User student1 = newUser("student", "张三", "STUDENT");
        User student2 = newUser("lisi", "李四", "STUDENT");
        User student3 = newUser("wangwu", "王五", "STUDENT");
        userMapper.insert(admin);
        userMapper.insert(teacher);
        userMapper.insert(student1);
        userMapper.insert(student2);
        userMapper.insert(student3);

        // 2. 课程
        Course c1 = newCourse("程序设计基础（C语言）", "CS101", teacher.getId(), "C 语言语法、控制结构与函数设计入门。", "3.0", "2026-2027 第一学期");
        Course c2 = newCourse("数据结构", "CS202", teacher.getId(), "线性表、树、图等数据结构及其算法。", "4.0", "2026-2027 第一学期");
        Course c3 = newCourse("网络空间安全导论", "SEC301", teacher.getId(), "密码学基础、网络安全体系与常见攻防模型。", "2.5", "2026-2027 第一学期");
        courseMapper.insert(c1);
        courseMapper.insert(c2);
        courseMapper.insert(c3);

        // 3. 作业
        Assignment a1 = newAssignment(c1.getId(), "实验一：Hello World 与基本输入输出", "编写程序实现姓名、学号输出，并提交源码与运行截图。", 100);
        Assignment a2 = newAssignment(c2.getId(), "作业一：链表的基本操作", "实现单链表的创建、插入、删除与遍历。", 100);
        Assignment a3 = newAssignment(c3.getId(), "读书报告：古典密码分析", "阅读教材并总结凯撒密码、维吉尼亚密码的加解密原理。", 100);
        assignmentMapper.insert(a1);
        assignmentMapper.insert(a2);
        assignmentMapper.insert(a3);

        // 4. 提交（含已批阅与待批阅，演示批阅反馈流程）
        Submission s1 = newSubmission(a1.getId(), student1.getId(), "已完成实验一，源码见附件。", 88, "思路清晰，注释完整，继续保持。");
        Submission s2 = newSubmission(a1.getId(), student2.getId(), "实验一提交。", null, null);
        Submission s3 = newSubmission(a2.getId(), student1.getId(), "链表操作已实现并通过测试。", 92, "对指针理解到位，注意边界情况。");
        Submission s4 = newSubmission(a3.getId(), student1.getId(), "已提交读书报告。", null, null);
        submissionMapper.insert(s1);
        submissionMapper.insert(s2);
        submissionMapper.insert(s3);
        submissionMapper.insert(s4);

        log.info("演示数据初始化完成，默认账号：admin / teacher / student，密码均为 123456");
    }

    private User newUser(String username, String realName, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        u.setStatus(1);
        return u;
    }

    private Course newCourse(String name, String code, Long teacherId, String desc, String credit, String semester) {
        Course c = new Course();
        c.setCourseName(name);
        c.setCourseCode(code);
        c.setTeacherId(teacherId);
        c.setDescription(desc);
        c.setCredit(new BigDecimal(credit));
        c.setSemester(semester);
        c.setStatus(1);
        return c;
    }

    private Assignment newAssignment(Long courseId, String title, String content, int totalScore) {
        Assignment a = new Assignment();
        a.setCourseId(courseId);
        a.setTitle(title);
        a.setContent(content);
        a.setDeadline(LocalDateTime.now().plusDays(7));
        a.setTotalScore(new BigDecimal(totalScore));
        return a;
    }

    private Submission newSubmission(Long assignmentId, Long studentId, String content, Integer score, String feedback) {
        Submission s = new Submission();
        s.setAssignmentId(assignmentId);
        s.setStudentId(studentId);
        s.setContent(content);
        s.setSubmitTime(LocalDateTime.now());
        if (score != null) {
            s.setScore(new BigDecimal(score));
            s.setFeedback(feedback);
            s.setStatus("GRADED");
            s.setGradeTime(LocalDateTime.now());
        } else {
            s.setStatus("SUBMITTED");
        }
        return s;
    }
}
