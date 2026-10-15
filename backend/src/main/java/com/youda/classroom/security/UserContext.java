package com.youda.classroom.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户上下文，使用 ThreadLocal 隔离线程，由 JwtInterceptor 写入
 */
public class UserContext {

    @Data
    @AllArgsConstructor
    public static class CurrentUser {
        private Long userId;
        private String username;
        private String role;
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static String getRole() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
