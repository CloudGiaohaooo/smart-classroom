package com.youda.classroom.service;

import com.youda.classroom.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录限流器单测：验证「窗口内放行 / 超阈值拒绝 / 成功清零」三条路径。
 * Redis 用 Mockito 隔离，直接控制计数返回值。
 */
class LoginRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private LoginRateLimiter limiter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        limiter = new LoginRateLimiter(redisTemplate);
        ReflectionTestUtils.setField(limiter, "maxAttempts", 5);
        ReflectionTestUtils.setField(limiter, "windowMinutes", 10);
    }

    @Test
    void allowsWithinThreshold() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        assertDoesNotThrow(() -> limiter.checkAllowed("alice", "1.2.3.4"));
    }

    @Test
    void rejectsWhenExceedsThreshold() {
        // 第 6 次（计数 6 > 阈值 5）起拒绝
        when(valueOps.increment(anyString())).thenReturn(6L);
        assertThrows(BusinessException.class, () -> limiter.checkAllowed("alice", "1.2.3.4"));
    }

    @Test
    void clearRemovesBothCountersOnSuccess() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        limiter.clear("alice", "1.2.3.4");
        verify(redisTemplate).delete("login:fail:user:alice");
        verify(redisTemplate).delete("login:fail:ip:1.2.3.4");
    }
}
