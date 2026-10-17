package com.youda.classroom.service;

import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录限流器：固定窗口计数，防暴力破解 / 撞库。
 * 登录接口是唯一无需鉴权即可访问的入口，最容易被字典或撞库攻击扫号。
 * 这里按「账号」与「客户端 IP」两个维度各自计数，任一超阈值即拒绝；登录成功清零。
 * Redis 不可用时降级放行（不因限流组件故障而阻断正常登录）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final String KEY_USER = "login:fail:user:";
    private static final String KEY_IP = "login:fail:ip:";

    private final StringRedisTemplate redisTemplate;

    @Value("${login.rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${login.rate-limit.window-minutes:10}")
    private long windowMinutes;

    /**
     * 登录前调用：检查并累加失败计数，超阈值抛出业务异常。
     */
    public void checkAllowed(String username, String ip) {
        if (exceeded(KEY_USER + username) || exceeded(KEY_IP + ip)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "登录尝试过于频繁，请稍后再试");
        }
    }

    /**
     * 登录成功后调用：清零该账号与 IP 的失败计数。
     */
    public void clear(String username, String ip) {
        delete(KEY_USER + username);
        delete(KEY_IP + ip);
    }

    private boolean exceeded(String key) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
            }
            return count != null && count > maxAttempts;
        } catch (Exception e) {
            log.warn("登录限流降级放行：{}", e.getMessage());
            return false;
        }
    }

    private void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("登录限流计数清除失败：{}", e.getMessage());
        }
    }
}
