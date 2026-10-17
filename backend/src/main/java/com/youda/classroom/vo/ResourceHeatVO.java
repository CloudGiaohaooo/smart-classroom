package com.youda.classroom.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源访问热度（Phase 4 看板：各课程资源浏览次数，来源于 AOP 埋点）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceHeatVO {

    /** 课程名称 */
    private String name;

    /** 浏览次数 */
    private Long count;
}
