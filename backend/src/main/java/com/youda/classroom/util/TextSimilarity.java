package com.youda.classroom.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 轻量文本相似度工具（创新点 A 核心）：纯 Java 实现 TF-IDF + 余弦相似度，不引入任何 NLP 依赖。
 *
 * 为什么不用大模型 / 重依赖：
 * 1. 作业雷同检测只需「同一作业下两段提交文本有多像」，是典型的短文本相似度问题；
 * 2. TF-IDF 衡量「词的重要程度」，余弦衡量「向量方向一致程度」，两者组合已能较好区分雷同与原创；
 * 3. 完全离线可跑，学生提交文本不出服务器，无隐私外泄风险（对比调用云端大模型）。
 *
 * 中文分词策略：不引入词典分词（jieba/hanlp 等），改用「字符 unigram + bigram」——
 * 连续汉字切成单字与双字滑窗，无需词典即可覆盖中文，轻量且对短文本足够有效。
 */
public final class TextSimilarity {

    private TextSimilarity() {
    }

    /**
     * 分词：归一化后生成 unigram（单字）+ bigram（双字滑窗）。
     * 只保留中日韩文字、ASCII 字母与数字，过滤标点/空白等噪声。
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>(normalized.length() * 2);
        for (int i = 0; i < normalized.length(); i++) {
            tokens.add(String.valueOf(normalized.charAt(i))); // unigram
            if (i + 1 < normalized.length()) {
                tokens.add(normalized.substring(i, i + 2));  // bigram
            }
        }
        return tokens;
    }

    /**
     * 计算两段文本在给定语料上的 TF-IDF 余弦相似度，结果落在 [0,1]。
     *
     * @param a      目标文本
     * @param b      待比较文本
     * @param corpus 语料（同一作业下的全部提交文本），用于计算 IDF
     */
    public static double tfIdfCosine(String a, String b, List<String> corpus) {
        // 1. 对语料所有文档分词，统计每个词的文档频率 DF
        Map<String, Integer> df = new HashMap<>();
        int n = 0;
        for (String doc : corpus) {
            if (doc == null) {
                continue;
            }
            n++;
            Set<String> unique = new HashSet<>(tokenize(doc));
            for (String t : unique) {
                df.merge(t, 1, Integer::sum);
            }
        }
        if (n == 0) {
            return 0;
        }
        // 2. 计算两段文本的 TF-IDF 向量
        Map<String, Double> va = tfIdfVector(tokenize(a), df, n);
        Map<String, Double> vb = tfIdfVector(tokenize(b), df, n);
        return cosine(va, vb);
    }

    /** 计算单个文档的 TF-IDF 向量：TF * IDF，IDF 用 log(1 + N/df) 平滑避免 df=N 时权重归零 */
    private static Map<String, Double> tfIdfVector(List<String> tokens, Map<String, Integer> df, int n) {
        Map<String, Double> tf = new HashMap<>();
        for (String t : tokens) {
            tf.merge(t, 1.0, Double::sum);
        }
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Double> e : tf.entrySet()) {
            int documentFreq = df.getOrDefault(e.getKey(), 1);
            double idf = Math.log(1.0 + (double) n / documentFreq);
            vector.put(e.getKey(), e.getValue() * idf);
        }
        return vector;
    }

    /** 两个稀疏向量的余弦相似度 */
    private static double cosine(Map<String, Double> va, Map<String, Double> vb) {
        double dot = 0;
        double normA = 0;
        for (Map.Entry<String, Double> e : va.entrySet()) {
            Double b = vb.get(e.getKey());
            if (b != null) {
                dot += e.getValue() * b;
            }
            normA += e.getValue() * e.getValue();
        }
        double normB = 0;
        for (double v : vb.values()) {
            normB += v * v;
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 归一化：小写化，仅保留 CJK / ASCII 字母 / 数字 */
    private static String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toLowerCase().toCharArray()) {
            if (isCjk(c) || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isCjk(char c) {
        return (c >= '一' && c <= '鿿') || (c >= '㐀' && c <= '䶿');
    }
}
