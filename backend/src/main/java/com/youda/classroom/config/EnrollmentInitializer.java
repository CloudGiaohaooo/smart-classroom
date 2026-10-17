package com.youda.classroom.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.entity.Course;
import com.youda.classroom.entity.CourseEnrollment;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.CourseEnrollmentMapper;
import com.youda.classroom.mapper.CourseMapper;
import com.youda.classroom.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课演示数据初始化器（Phase 18 选课模型）：
 * 幂等地把演示学生写入 course_enrollment，保证「学生只看/交自己选的课」在答辩现场开箱即用。
 *
 * - 核心演示学生 student / lisi / wangwu → 选全部课程；
 * - 雷同检测的 20 名 demo01~demo20 → 选第一门课程（与其成簇提交归属一致）。
 *
 * 每次启动都检查缺失项并补齐（同 RbacInitializer），因此对「老库升级」同样有效。
 * @Order(4)：在 DataInitializer(1) / RbacInitializer(2) / SimilarityDemoDataInitializer(3) 之后执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class EnrollmentInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    @Override
    public void run(String... args) {
        List<Course> courses = courseMapper.selectList(null);
        if (courses.isEmpty()) {
            return;
        }
        for (String username : new String[]{"student", "lisi", "wangwu"}) {
            enrollAll(username, courses);
        }
        // 雷同检测演示学生只修第一门课，与其提交数据一致
        Course first = courses.get(0);
        for (int i = 1; i <= 20; i++) {
            enrollAll(String.format("demo%02d", i), List.of(first));
        }
    }

    private void enrollAll(String username, List<Course> courses) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return;
        }
        for (Course course : courses) {
            Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                    .eq(CourseEnrollment::getCourseId, course.getId())
                    .eq(CourseEnrollment::getStudentId, user.getId()));
            if (count == null || count == 0) {
                CourseEnrollment ce = new CourseEnrollment();
                ce.setCourseId(course.getId());
                ce.setStudentId(user.getId());
                ce.setEnrollTime(LocalDateTime.now());
                enrollmentMapper.insert(ce);
            }
        }
    }
}
