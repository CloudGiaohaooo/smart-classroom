package com.youda.classroom.vo;

import lombok.Data;

/**
 * 相似提交视图对象（创新点 A：雷同检测结果）
 */
@Data
public class SimilarSubmissionVO {

    private Long submissionId;

    private Long studentId;

    private String studentName;

    /** 相似度（0~100 百分比，TF-IDF 余弦 * 100 后四舍五入到 1 位小数） */
    private Double similarity;

    /** 是否疑似雷同（相似度 >= 疑似阈值） */
    private Boolean flagged;

    /** 提交内容摘要（截断展示） */
    private String excerpt;
}
