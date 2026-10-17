package com.youda.classroom.cache;

/**
 * 缓存键常量集中管理（收敛原先散落在各 Service 的魔法字符串，避免跨类耦合）。
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 课程全量列表（下拉选择、看板热点读） */
    public static final String COURSE_LIST = "cache:course:all";

    /** 看板聚合统计（仪表盘首页热点读，涉及多表 COUNT） */
    public static final String STATS_OVERVIEW = "cache:stats:overview";
}
