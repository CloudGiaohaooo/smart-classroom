package com.youda.classroom.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 轻量级表结构迁移（幂等，老库升级专用）。
 * schema.sql 的 CREATE TABLE IF NOT EXISTS 不会修改已存在的表，因此针对「老库升级」场景，
 * 用 information_schema 幂等判断后补充缺列/缺索引：
 *  1. course_resource 补 file_md5 列（MD5 秒传去重）；
 *  2. submission 先去重再补唯一索引 uk_assignment_student（防重复提交）。
 * 排在 DataInitializer(@Order(1)) 之前执行，保证后续插入/查询可用这些结构。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        migrateCourseResourceFileMd5();
        migrateSubmissionUniqueIndex();
    }

    /** 老库补 file_md5 列（MD5 秒传去重） */
    private void migrateCourseResourceFileMd5() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_resource' AND COLUMN_NAME = 'file_md5'",
                Integer.class);
        if (count != null && count > 0) {
            return;
        }
        log.info("表结构迁移：为 course_resource 新增 file_md5 列（MD5 秒传去重）");
        jdbcTemplate.execute("ALTER TABLE course_resource " +
                "ADD COLUMN file_md5 VARCHAR(32) DEFAULT NULL COMMENT '文件MD5（秒传去重标识）' AFTER file_size, " +
                "ADD KEY idx_file_md5 (file_md5)");
    }

    /**
     * 老库补 submission 唯一索引（防重复提交）。
     * schema.sql 的 CREATE TABLE IF NOT EXISTS 不会给已存在的表加索引，因此针对老库：
     * 先按 (assignment_id, student_id) 去重（保留最早一条），再幂等加唯一索引，避免并发窗口重复提交。
     */
    private void migrateSubmissionUniqueIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'submission' AND INDEX_NAME = 'uk_assignment_student'",
                Integer.class);
        if (count != null && count > 0) {
            return;
        }
        log.info("表结构迁移：submission 去重并新增唯一索引 uk_assignment_student（防重复提交）");
        // MySQL 多表删除：同一 (assignment_id, student_id) 保留 id 最小（最早）的一条，删除其余重复记录
        jdbcTemplate.execute(
                "DELETE s1 FROM submission s1 " +
                "INNER JOIN submission s2 " +
                "ON s1.assignment_id = s2.assignment_id AND s1.student_id = s2.student_id AND s1.id > s2.id");
        jdbcTemplate.execute("ALTER TABLE submission ADD UNIQUE KEY uk_assignment_student (assignment_id, student_id)");
    }
}
