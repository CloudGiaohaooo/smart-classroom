package com.youda.classroom.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文本相似度工具单测（创新点 A 核心算法）：
 * 验证 TF-IDF + 余弦在「同簇高相似 / 跨簇低相似」上的区分度。
 */
class TextSimilarityTest {

    @Test
    void tokenizeProducesUnigramAndBigram() {
        List<String> tokens = TextSimilarity.tokenize("冒泡排序");
        assertTrue(tokens.contains("冒"), "应包含 unigram 单字");
        assertTrue(tokens.contains("冒泡"), "应包含 bigram 双字");
        assertTrue(tokens.contains("泡"), "应包含单字");
    }

    @Test
    void tokenizeFiltersNoise() {
        List<String> tokens = TextSimilarity.tokenize("A, b。!");
        assertTrue(tokens.contains("a"), "应保留字母并小写化");
        assertTrue(!tokens.contains(","), "应过滤标点");
    }

    @Test
    void identicalTextSimilarityIsOne() {
        String text = "本程序使用冒泡排序对整型数组进行升序排列，将排序逻辑封装在 sort 函数中。";
        double s = TextSimilarity.tfIdfCosine(text, text, List.of(text));
        assertEquals(1.0, s, 1e-9);
    }

    @Test
    void sameClusterHighSimilarityDifferentTopicLow() {
        String base = "本程序使用冒泡排序对整型数组进行升序排列，将排序逻辑封装在 sort 函数中。";
        String same = base + " 运行结果正确。";
        String unrelated = "今天天气很好，我去了图书馆看书。";
        List<String> corpus = List.of(base, same, unrelated, "另一段无关的文本内容");

        double high = TextSimilarity.tfIdfCosine(base, same, corpus);
        double low = TextSimilarity.tfIdfCosine(base, unrelated, corpus);

        assertTrue(high > 0.7, "同簇文本相似度应较高，实际=" + high);
        assertTrue(low < high, "跨主题文本相似度应更低");
    }

    @Test
    void emptyInputReturnsZero() {
        assertEquals(0.0, TextSimilarity.tfIdfCosine("", "text", List.of("text")), 1e-9);
        assertEquals(0.0, TextSimilarity.tfIdfCosine("a", "b", List.of()), 1e-9);
        assertEquals(0.0, TextSimilarity.tfIdfCosine(null, "b", List.of("a", "b")), 1e-9);
    }

    @Test
    void precomputedVectorsMatchSingleShot() {
        String a = "本程序使用冒泡排序对整型数组进行升序排列。";
        String b = "本程序使用选择排序对整型数组进行升序排列。";
        String c = "今天天气很好，我去图书馆看书。";
        List<String> corpus = List.of(a, b, c);

        Map<String, Integer> df = TextSimilarity.documentFrequency(corpus);
        int n = corpus.size();
        Map<String, Double> va = TextSimilarity.tfIdfVector(a, df, n);
        Map<String, Double> vb = TextSimilarity.tfIdfVector(b, df, n);

        // 预计算向量 + 余弦，与一次性 tfIdfCosine 结果一致（验证 O(n²·L) 优化等价）
        assertEquals(TextSimilarity.tfIdfCosine(a, b, corpus), TextSimilarity.cosine(va, vb), 1e-9);
        assertEquals(TextSimilarity.tfIdfCosine(a, c, corpus),
                TextSimilarity.cosine(va, TextSimilarity.tfIdfVector(c, df, n)), 1e-9);
    }
}
