package com.youda.classroom.annotation;

import com.youda.classroom.enums.BehaviorType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 学习行为埋点注解（Phase 4 创新点 B）：
 * 标注在 Controller 方法上，由 {@code BehaviorLogAspect} 统一记录到 student_behavior 表。
 * 业务代码只需加一行注解，埋点逻辑与业务完全解耦。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BehaviorLog {

    /** 行为类型 */
    BehaviorType value();
}
