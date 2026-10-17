package com.youda.classroom.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Redis 缓存服务（Phase 2）：集中落地缓存「三件套」策略，并对 Redis 宕机做优雅降级。
 *
 * 三策略说明（答辩重点）：
 * 1. 缓存穿透（Penetration）：查询不存在的数据时，把「空值标记 __NULL__」也写入缓存（短 TTL），
 *    避免恶意/高频访问不存在的 key 每次都打到数据库；
 * 2. 缓存击穿（Breakdown）：热点 key 过期瞬间大量请求同时重建，用 SETNX 互斥锁保证同一时刻
 *    只有一个请求回源重建，其余请求短暂等待后重读；
 * 3. 缓存雪崩（Avalanche）：大量 key 同时过期导致数据库瞬时压力，给每个 key 的 TTL 叠加随机抖动，
 *    打散过期时间点。
 *
 * 降级：所有 Redis 操作都包在 try/catch 内，Redis 连接失败时自动跳过缓存直查数据库，
 *       保证「关闭 Redis 后接口不 500」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    /** 空值缓存标记（防穿透：区分「缓存里确实没有」与「缓存里存了空」） */
    private static final String NULL_MARKER = "__NULL__";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.null-value-ttl}")
    private long nullValueTtl;

    @Value("${cache.lock-ttl}")
    private long lockTtl;

    @Value("${cache.ttl-jitter}")
    private long ttlJitter;

    /**
     * 通用缓存读取：命中返回，未命中走 loader 回源重建并回填。
     *
     * @param key           缓存键
     * @param type          反序列化目标类型（支持泛型，如 new TypeReference<List<Course>>(){}）
     * @param baseTtlSeconds 基础 TTL（秒），实际写入时叠加随机抖动（防雪崩）
     * @param loader        回源函数（查数据库）
     */
    public <T> T getOrLoad(String key, TypeReference<T> type, long baseTtlSeconds, Supplier<T> loader) {
        // 1. 先读缓存
        String cached = safeGet(key);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                return null; // 命中空值缓存（防穿透）
            }
            T value = deserialize(cached, type);
            if (value != null) {
                return value;
            }
        }

        // 2. 未命中，尝试互斥锁重建（防击穿）
        String lockKey = key + ":lock";
        if (tryLock(lockKey)) {
            try {
                // double-check：拿到锁后可能已被其它线程重建
                cached = safeGet(key);
                if (cached != null) {
                    if (NULL_MARKER.equals(cached)) {
                        return null;
                    }
                    T value = deserialize(cached, type);
                    if (value != null) {
                        return value;
                    }
                }
                // 回源查询
                T loaded = loader.get();
                if (isEmpty(loaded)) {
                    // 空结果也写缓存，短 TTL（防穿透）
                    safeSet(key, NULL_MARKER, nullValueTtl);
                } else {
                    // 正常结果写缓存，基础 TTL + 随机抖动（防雪崩）
                    safeSet(key, serialize(loaded), baseTtlSeconds + randomJitter());
                }
                return loaded;
            } finally {
                unlock(lockKey);
            }
        }

        // 3. 未抢到锁：短暂等待后重读一次，仍无则直查库兜底（不长时间阻塞）
        sleepQuietly(50);
        cached = safeGet(key);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                return null;
            }
            T value = deserialize(cached, type);
            if (value != null) {
                return value;
            }
        }
        return loader.get();
    }

    /** 删除缓存（写操作后调用，保证缓存与数据库一致） */
    public void evict(String key) {
        safeDelete(key);
    }

    /**
     * 尝试获取分布式互斥锁（Redis SETNX），供「先查后写」类临界区使用（如秒传去重、资源删除）。
     * 返回 true 表示抢到锁（调用方需在 finally 中 {@link #release}）；Redis 不可用时返回 false（视为未加锁，直接放行）。
     */
    public boolean tryAcquire(String key, long ttlSeconds) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds)));
        } catch (Exception e) {
            log.warn("Redis 加锁失败，降级为无锁执行：{}", e.getMessage());
            return false;
        }
    }

    /** 释放 {@link #tryAcquire} 获取的锁。 */
    public void release(String key) {
        safeDelete(key);
    }

    // ==================== 底层安全操作：Redis 不可用一律降级 ====================

    private String safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败，降级直查数据库：{}", e.getMessage());
            return null;
        }
    }

    private void safeSet(String key, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Redis 写入失败：{}", e.getMessage());
        }
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除失败：{}", e.getMessage());
        }
    }

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtl));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // Redis 不可用：无法加锁，返回 true 直接走重建（safeGet/safeSet 会自行降级）
            return true;
        }
    }

    private void unlock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("Redis 解锁失败：{}", e.getMessage());
        }
    }

    private long randomJitter() {
        return ttlJitter <= 0 ? 0 : ThreadLocalRandom.current().nextLong(ttlJitter + 1);
    }

    private <T> String serialize(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("缓存序列化失败", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败：{}", e.getMessage());
            return null;
        }
    }

    private <T> boolean isEmpty(T value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
