-- 优达智慧课堂教学管理平台 数据库表结构
-- 说明：外键约束在应用层通过业务代码 + 索引保证，便于演示与分库分表扩展

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `real_name`   VARCHAR(50)  NOT NULL COMMENT '姓名',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN/TEACHER/STUDENT',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `course` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_name` VARCHAR(100)  NOT NULL COMMENT '课程名称',
    `course_code` VARCHAR(50)   DEFAULT NULL COMMENT '课程编号',
    `teacher_id`  BIGINT        NOT NULL COMMENT '授课教师ID（关联 sys_user.id）',
    `description` VARCHAR(500)  DEFAULT NULL COMMENT '课程简介',
    `credit`      DECIMAL(3,1)  DEFAULT 0 COMMENT '学分',
    `semester`    VARCHAR(30)   DEFAULT NULL COMMENT '开课学期',
    `status`      TINYINT       DEFAULT 1 COMMENT '状态：1进行中 0已结课',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE IF NOT EXISTS `course_resource` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`     BIGINT       NOT NULL COMMENT '所属课程ID（关联 course.id）',
    `resource_name` VARCHAR(200) NOT NULL COMMENT '资源名称',
    `resource_type` VARCHAR(20)  DEFAULT 'OTHER' COMMENT '资源类型：VIDEO/PDF/DOC/PPT/OTHER',
    `file_url`      VARCHAR(500) DEFAULT NULL COMMENT '文件访问路径',
    `file_size`     BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    `file_md5`      VARCHAR(32)  DEFAULT NULL COMMENT '文件MD5（秒传去重标识）',
    `uploader_id`   BIGINT       DEFAULT NULL COMMENT '上传人ID',
    `upload_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_file_md5` (`file_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资源表';

CREATE TABLE IF NOT EXISTS `assignment` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`   BIGINT        NOT NULL COMMENT '所属课程ID（关联 course.id）',
    `title`       VARCHAR(200)  NOT NULL COMMENT '作业标题',
    `content`     TEXT          COMMENT '作业要求',
    `deadline`    DATETIME      DEFAULT NULL COMMENT '截止时间',
    `total_score` DECIMAL(5,2)  DEFAULT 100 COMMENT '满分',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

CREATE TABLE IF NOT EXISTS `submission` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `assignment_id` BIGINT        NOT NULL COMMENT '作业ID（关联 assignment.id）',
    `student_id`    BIGINT        NOT NULL COMMENT '学生ID（关联 sys_user.id）',
    `content`       TEXT          COMMENT '提交内容',
    `file_url`      VARCHAR(500)  DEFAULT NULL COMMENT '提交附件地址',
    `submit_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `score`         DECIMAL(5,2)  DEFAULT NULL COMMENT '得分',
    `feedback`      VARCHAR(1000) DEFAULT NULL COMMENT '批阅反馈',
    `status`        VARCHAR(20)   DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED已提交/GRADED已批阅',
    `grade_time`    DATETIME      DEFAULT NULL COMMENT '批阅时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assignment_student` (`assignment_id`, `student_id`),
    KEY `idx_assignment_id` (`assignment_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

CREATE TABLE IF NOT EXISTS `course_enrollment` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`   BIGINT   NOT NULL COMMENT '课程ID（关联 course.id）',
    `student_id`  BIGINT   NOT NULL COMMENT '学生ID（关联 sys_user.id）',
    `enroll_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课表';

-- =============================================================
-- RBAC 权限模型（Phase 2）
-- 说明：原 sys_user.role 仍保留（JWT 携带、前端菜单过滤用），
--       RBAC 四表用于后端细粒度授权（Spring Security @PreAuthorize）。
--       admin/teacher/student 通过 sys_user_role 映射到角色，角色再关联权限。
-- =============================================================

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码：ADMIN/TEACHER/STUDENT',
    `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `perm_code`   VARCHAR(100) NOT NULL COMMENT '权限编码，如 course:write',
    `perm_name`   VARCHAR(100) NOT NULL COMMENT '权限名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（关联 sys_user.id）',
    `role_id` BIGINT NOT NULL COMMENT '角色ID（关联 sys_role.id）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`       BIGINT NOT NULL COMMENT '角色ID（关联 sys_role.id）',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID（关联 sys_permission.id）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- =============================================================
-- 学习行为埋点表（Phase 4 创新点 B）
-- 说明：由 BehaviorLogAspect 切面写入，业务代码仅需 @BehaviorLog 注解；
--       预警规则阈值外置在 application.yml（warning.*）。
-- =============================================================

CREATE TABLE IF NOT EXISTS `student_behavior` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`    BIGINT       NOT NULL COMMENT '学生ID（关联 sys_user.id）',
    `behavior_type` VARCHAR(32)  NOT NULL COMMENT '行为类型：LOGIN/VIEW_RESOURCE/SUBMIT_ASSIGNMENT',
    `target_id`     BIGINT       DEFAULT NULL COMMENT '目标对象ID：资源->课程ID / 提交->作业ID',
    `detail`        VARCHAR(100) DEFAULT NULL COMMENT '行为描述',
    `occur_time`    DATETIME     NOT NULL COMMENT '发生时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_time` (`student_id`, `occur_time`),
    KEY `idx_type_time` (`behavior_type`, `occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生行为埋点表';
