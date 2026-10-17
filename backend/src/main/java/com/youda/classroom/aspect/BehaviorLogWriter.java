package com.youda.classroom.aspect;

import com.youda.classroom.entity.StudentBehavior;
import com.youda.classroom.mapper.StudentBehaviorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 学习行为埋点写入器：@Async 异步落库，与业务主流程解耦。
 * 由 {@link BehaviorLogAspect} 在 @AfterReturning 中调用，埋点写入不再放大请求 RT。
 */
@Component
@RequiredArgsConstructor
public class BehaviorLogWriter {

    private final StudentBehaviorMapper behaviorMapper;

    @Async("behaviorExecutor")
    public void write(StudentBehavior behavior) {
        behaviorMapper.insert(behavior);
    }
}
