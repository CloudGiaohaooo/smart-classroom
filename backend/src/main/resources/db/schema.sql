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
    `uploader_id`   BIGINT       DEFAULT NULL COMMENT '上传人ID',
    `upload_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`)
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
    KEY `idx_assignment_id` (`assignment_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';
