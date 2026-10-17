package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.util.TextSimilarity;
import com.youda.classroom.vo.FeedbackSuggestionVO;
import com.youda.classroom.vo.GradingAssistVO;
import com.youda.classroom.vo.SimilarSubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 批阅辅助服务（创新点 A）：
 * 1. 相似提交检测：对同一作业下的学生提交做 TF-IDF + 余弦相似度计算，标记疑似雷同；
 * 2. 评语推荐：基于教师历史批阅评语做频率统计，给出 Top N 推荐（同作业优先，不足再全局补齐）。
 *
 * 评分提交仍走原 SubmissionService.grade 的 @Transactional 链路，本服务只读、不写，
 * 因此不破坏原有的原子性保证。
 */
@Service
@RequiredArgsConstructor
public class GradingAssistService {

    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final AssignmentMapper assignmentMapper;
    private final CourseOwnershipGuard ownershipGuard;

    @Value("${similarity.threshold}")
    private double threshold;

    @Value("${similarity.suspicious-threshold}")
    private double suspiciousThreshold;

    @Value("${similarity.top-n}")
    private int topN;

    @Value("${similarity.suggestion-limit}")
    private int suggestionLimit;

    /**
     * 批阅辅助入口：一次返回相似提交 + 推荐评语
     */
    public GradingAssistVO assist(Long submissionId) {
        Submission target = submissionMapper.selectById(submissionId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提交记录不存在");
        }
        // 越权防护：批阅辅助同样受课程归属约束，防止教师读取其它课程的提交摘要/学生姓名/相似度
        Assignment assignment = assignmentMapper.selectById(target.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在");
        }
        ownershipGuard.assertCourseOwned(assignment.getCourseId());
        List<Submission> all = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, target.getAssignmentId()));
        return new GradingAssistVO(computeSimilar(target, all), suggestFeedback(target, all));
    }

    /**
     * 雷同检测：当前提交与同作业其它提交两两计算 TF-IDF 余弦相似度，返回超过阈值的（降序）
     */
    private List<SimilarSubmissionVO> computeSimilar(Submission target, List<Submission> all) {
        String targetText = nvl(target.getContent());
        List<Submission> others = all.stream()
                .filter(s -> !s.getId().equals(target.getId()))
                .toList();
        Map<Long, String> nameMap = loadStudentNames(others);

        // 语料 = 该作业全部提交文本（用于 IDF 统计）
        // 性能优化：DF 与每篇提交的 TF-IDF 向量各算一次（O(n·L)），再逐对求余弦（O(n·d)），
        // 避免旧实现「每个候选都重新对整个语料分词 + 重算 DF」导致的 O(n²·L)。
        List<String> corpus = all.stream().map(s -> nvl(s.getContent())).toList();
        Map<String, Integer> df = TextSimilarity.documentFrequency(corpus);
        int n = corpus.size();
        Map<String, Double> targetVec = TextSimilarity.tfIdfVector(targetText, df, n);
        Map<Long, Map<String, Double>> vecBySubmission = new HashMap<>();
        for (Submission s : all) {
            vecBySubmission.put(s.getId(), TextSimilarity.tfIdfVector(nvl(s.getContent()), df, n));
        }

        List<SimilarSubmissionVO> result = new ArrayList<>();
        for (Submission other : others) {
            double sim = TextSimilarity.cosine(targetVec, vecBySubmission.getOrDefault(other.getId(), Map.of()));
            if (sim < threshold) {
                continue;
            }
            SimilarSubmissionVO vo = new SimilarSubmissionVO();
            vo.setSubmissionId(other.getId());
            vo.setStudentId(other.getStudentId());
            vo.setStudentName(nameMap.get(other.getStudentId()));
            vo.setSimilarity(round(sim * 100, 1)); // 转百分比
            vo.setFlagged(sim >= suspiciousThreshold);
            vo.setExcerpt(excerpt(other.getContent(), 60));
            result.add(vo);
        }
        result.sort(Comparator.comparingDouble(SimilarSubmissionVO::getSimilarity).reversed());
        return result.size() > topN ? result.subList(0, topN) : result;
    }

    /**
     * 评语推荐：同一作业下已批阅评语按使用频率降序取 Top N，不足再全局历史补齐
     */
    private List<FeedbackSuggestionVO> suggestFeedback(Submission target, List<Submission> all) {
        List<Submission> gradedSame = all.stream()
                .filter(s -> !s.getId().equals(target.getId()))
                .filter(s -> s.getFeedback() != null && !s.getFeedback().isBlank())
                .toList();
        Map<String, List<Submission>> byFeedback = gradedSame.stream()
                .collect(Collectors.groupingBy(Submission::getFeedback));

        List<FeedbackSuggestionVO> suggestions = new ArrayList<>();
        for (Map.Entry<String, List<Submission>> e : sortByCountDesc(byFeedback)) {
            suggestions.add(toSuggestion(e.getKey(), e.getValue()));
            if (suggestions.size() >= suggestionLimit) {
                break;
            }
        }

        // 同作业评语不足 Top N，用全库历史评语补齐（排除已选中的）
        if (suggestions.size() < suggestionLimit) {
            Set<String> existing = suggestions.stream()
                    .map(FeedbackSuggestionVO::getText).collect(Collectors.toSet());
            // 全局补齐限定「本人授课课程」的已批阅提交，防止把其它教师的评语原文泄露给当前教师
            List<Submission> gradedAll = submissionMapper.selectGradedByTeacher(UserContext.getUserId());
            Map<String, List<Submission>> global = gradedAll.stream()
                    .collect(Collectors.groupingBy(Submission::getFeedback));
            for (Map.Entry<String, List<Submission>> e : sortByCountDesc(global)) {
                if (existing.contains(e.getKey())) {
                    continue;
                }
                suggestions.add(toSuggestion(e.getKey(), e.getValue()));
                if (suggestions.size() >= suggestionLimit) {
                    break;
                }
            }
        }
        return suggestions;
    }

    private FeedbackSuggestionVO toSuggestion(String text, List<Submission> subs) {
        FeedbackSuggestionVO vo = new FeedbackSuggestionVO();
        vo.setText(text);
        vo.setCount((long) subs.size());
        double avg = subs.stream()
                .map(Submission::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);
        vo.setAvgScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        return vo;
    }

    private List<Map.Entry<String, List<Submission>>> sortByCountDesc(Map<String, List<Submission>> map) {
        return map.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .toList();
    }

    /** 批量查询学生姓名，避免 N+1 查询 */
    private Map<Long, String> loadStudentNames(List<Submission> subs) {
        Set<Long> ids = subs.stream().map(Submission::getStudentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User u : userMapper.selectBatchIds(ids)) {
            map.put(u.getId(), u.getRealName());
        }
        return map;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private double round(double v, int places) {
        return BigDecimal.valueOf(v).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private String excerpt(String content, int max) {
        String s = nvl(content).replaceAll("\\s+", " ");
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
