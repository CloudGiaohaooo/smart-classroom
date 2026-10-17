package com.youda.classroom.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交率趋势点（Phase 4 看板：近 N 天每日提交量）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTrendPointVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 当日提交次数 */
    private Long count;
}
