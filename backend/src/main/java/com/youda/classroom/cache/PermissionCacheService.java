package com.youda.classroom.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限编码本地缓存（Phase 19，复审 v3「本周」#7：权限缓存）。
 *
 * JwtAuthenticationFilter 每个请求都要加载 userId -> 权限编码列表，是该过滤链上每请求一次 DB 查询的
 * 主要来源。这里用 Caffeine 纯内存缓存把权限编码列表缓存一段时间，命中后跳过本次 DB 查询。
 *
 * 取舍（见 docs/improvement-plan.md Phase 19）：
 * - 账号状态（userMapper.selectById）仍每请求复查，保证「禁用账号即时失效」特性不受缓存影响；
 * - 权限变更（改角色/改权限）最多延迟一个 TTL（默认 300s）生效，属可接受的最终一致。
 *
 * TTL 外置可调（permission-cache.ttl），不硬编码魔法值。
 */
@Component
public class PermissionCacheService {

    private final Cache<Long, List<String>> cache;

    public PermissionCacheService(@Value("${permission-cache.ttl:300}") long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    /** 缓存命中返回权限编码列表，未命中返回 null（由调用方回源 DB 并回填）。 */
    public List<String> get(Long userId) {
        if (userId == null) {
            return null;
        }
        return cache.getIfPresent(userId);
    }

    public void put(Long userId, List<String> permCodes) {
        if (userId != null && permCodes != null) {
            cache.put(userId, permCodes);
        }
    }

    /** 主动失效：后续（如管理员改角色/改权限后）可调用，使变更即时生效。 */
    public void evict(Long userId) {
        if (userId != null) {
            cache.invalidate(userId);
        }
    }
}
