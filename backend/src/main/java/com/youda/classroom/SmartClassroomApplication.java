package com.youda.classroom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 优达智慧课堂教学管理平台 - 启动类
 */
@SpringBootApplication
@MapperScan("com.youda.classroom.mapper")
public class SmartClassroomApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartClassroomApplication.class, args);
    }
}
