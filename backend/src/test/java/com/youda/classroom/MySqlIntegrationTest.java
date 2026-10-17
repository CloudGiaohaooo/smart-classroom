package com.youda.classroom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.dto.AssignmentDTO;
import com.youda.classroom.dto.CourseDTO;
import com.youda.classroom.dto.GradeDTO;
import com.youda.classroom.dto.SubmitDTO;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.service.AssignmentService;
import com.youda.classroom.service.CourseService;
import com.youda.classroom.service.EnrollmentService;
import com.youda.classroom.service.SubmissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testcontainers 真实 MySQL 容器集成测试（复审 v3 本周 #8）。
 *
 * 与纯 Mockito / MockMvc 单测不同，本类启动完整 Spring 上下文并连到真实 MySQL 8 容器，
 * 验证：schema.sql 建表 + 演示数据初始化、Service 到真实数据库的完整落库链路、
 * 唯一索引（选课去重 / 提交去重）在数据库层真正生效、以及横向越权校验在完整上下文中的行为。
 *
 * 本地未安装 Docker 时自动跳过（disabledWithoutDocker），不影响常规 mvn test。
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlIntegrationTest {

    /** 与 docker-compose.yml 保持一致：mysql:8.0、专用库 youda_classroom、专用账号 youda */
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("youda_classroom")
            .withUsername("youda")
            .withPassword("youda_pass_2026");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                MYSQL.getJdbcUrl() + "?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private AssignmentMapper assignmentMapper;
    @Autowired
    private SubmissionMapper submissionMapper;
    @Autowired
    private CourseService courseService;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private SubmissionService submissionService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void schemaAndDemoDataAreLoadedAgainstRealMysql() {
        // schema.sql 已在上下文启动时对容器库执行；演示账号由 DataInitializer 幂等写入
        assertTrue(userMapper.selectCount(null) > 0, "演示用户应已初始化");
        assertNotNull(userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "teacher")));
        assertNotNull(userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "student")));
        assertNotNull(userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")));
    }

    @Test
    void fullTeachingFlowPersistsAndGuardsAgainstHorizontalAccess() {
        User teacher = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "teacher"));
        User student = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "student"));

        // 1. 教师建课（teacher_id 由 CourseService 强置为本人）
        UserContext.set(new UserContext.CurrentUser(teacher.getId(), "teacher", "TEACHER"));
        CourseDTO courseDTO = new CourseDTO();
        courseDTO.setCourseName("集成测试-程序设计");
        courseDTO.setCourseCode("IT-001");
        courseDTO.setCredit(new BigDecimal("2.0"));
        courseDTO.setSemester("2026-2027 第一学期");
        courseService.create(courseDTO);

        Course course = courseMapper.selectOne(
                new LambdaQueryWrapper<Course>().eq(Course::getCourseName, "集成测试-程序设计"));
        assertNotNull(course, "课程应已落库");
        assertEquals(teacher.getId(), course.getTeacherId(), "建课归属应为当前教师");

        // 2. 教师布置作业
        AssignmentDTO assignmentDTO = new AssignmentDTO();
        assignmentDTO.setCourseId(course.getId());
        assignmentDTO.setTitle("集成测试-作业一");
        assignmentDTO.setContent("完成实验并提交源码");
        assignmentDTO.setDeadline(LocalDateTime.now().plusDays(7));
        assignmentDTO.setTotalScore(new BigDecimal("100"));
        assignmentService.create(assignmentDTO);

        Assignment assignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<Assignment>().eq(Assignment::getTitle, "集成测试-作业一"));
        assertNotNull(assignment, "作业应已落库");

        // 3. 学生选课 + 真实唯一索引 (course_id, student_id) 防重复选课
        UserContext.set(new UserContext.CurrentUser(student.getId(), "student", "STUDENT"));
        enrollmentService.enroll(course.getId());
        assertTrue(enrollmentService.isEnrolled(course.getId()), "选课应生效");
        BusinessException duplicateEnroll = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(course.getId()), "重复选课应被唯一索引兜底拒绝");
        assertEquals(400, duplicateEnroll.getCode());

        // 4. 学生提交作业 + 真实唯一索引 (assignment_id, student_id) 防重复提交
        SubmitDTO submitDTO = new SubmitDTO();
        submitDTO.setAssignmentId(assignment.getId());
        submitDTO.setContent("集成测试提交内容");
        submissionService.submit(submitDTO);
        assertThrows(BusinessException.class,
                () -> submissionService.submit(submitDTO), "重复提交应被唯一索引兜底拒绝");

        // 5. 教师批阅（真实事务落库）
        UserContext.set(new UserContext.CurrentUser(teacher.getId(), "teacher", "TEACHER"));
        Submission submission = submissionMapper.selectOne(
                new LambdaQueryWrapper<Submission>().eq(Submission::getAssignmentId, assignment.getId()));
        GradeDTO gradeDTO = new GradeDTO();
        gradeDTO.setScore(new BigDecimal("95"));
        gradeDTO.setFeedback("完成度高，注意边界用例");
        submissionService.grade(submission.getId(), gradeDTO);

        Submission graded = submissionMapper.selectById(submission.getId());
        assertEquals("GRADED", graded.getStatus());
        assertEquals(0, new BigDecimal("95").compareTo(graded.getScore()));

        // 6. 横向越权：学生（非授课教师）不得删除课程下的作业，完整上下文下仍被 CourseOwnershipGuard 拦截
        UserContext.set(new UserContext.CurrentUser(student.getId(), "student", "STUDENT"));
        assertThrows(BusinessException.class,
                () -> assignmentService.delete(assignment.getId()), "非授课教师越权删除作业应被拒绝");
    }
}
