package com.youda.classroom.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐评语视图对象（创新点 A：历史评语推荐）
 */
@Data
public class FeedbackSuggestionVO {

    /** 推荐评语文本 */
    private String text;

    /** 该评语在历史批阅中被使用的次数（频率越高越推荐） */
    private Long count;

    /** 该评语通常伴随的平均得分（可选，帮助教师一键填入时参考） */
    private BigDecimal avgScore;
}
