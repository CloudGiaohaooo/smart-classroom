package com.youda.classroom.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 批阅辅助视图对象（创新点 A）：一次返回「相似提交」与「推荐评语」两部分，供批阅页渲染
 */
@Data
@AllArgsConstructor
public class GradingAssistVO {

    /** 相似提交列表（雷同检测结果，按相似度降序） */
    private List<SimilarSubmissionVO> similar;

    /** 推荐评语列表（按历史使用频率降序，Top N） */
    private List<FeedbackSuggestionVO> suggestions;
}
