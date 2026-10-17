package com.youda.classroom.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 轻量级表结构迁移（创新点 C：MD5 秒传去重）。
 * schema.sql 的 CREATE TABLE IF NOT EXISTS 不会修改已存在的表，
 * 因此针对「老库升级」场景，用 information_schema 幂等判断后补充 file_md5 列。
 * 排在 DataInitializer(@Order(1)) 之前执行，保证后续插入/查询可用该列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
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
}
