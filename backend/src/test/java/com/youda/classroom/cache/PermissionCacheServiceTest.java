package com.youda.classroom.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 权限缓存单测（Phase 19）：验证 Caffeine 缓存命中 / 回填 / 主动失效。
 */
class PermissionCacheServiceTest {

    @Test
    void cacheStoresAndReturnsPermissions() {
        PermissionCacheService cache = new PermissionCacheService(300);

        assertNull(cache.get(1L));
        cache.put(1L, List.of("course:read", "course:write"));

        assertEquals(List.of("course:read", "course:write"), cache.get(1L));
    }

    @Test
    void evictRemovesEntry() {
        PermissionCacheService cache = new PermissionCacheService(300);

        cache.put(1L, List.of("course:read"));
        cache.evict(1L);

        assertNull(cache.get(1L));
    }

    @Test
    void nullUserIdIsSafe() {
        PermissionCacheService cache = new PermissionCacheService(300);

        assertNull(cache.get(null));
        cache.put(null, List.of("course:read")); // 不应抛异常，直接忽略
        cache.evict(null);                        // 不应抛异常，直接忽略
    }
}
