package com.youda.classroom.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雷同检测离线评测（创新点 A 的证据链，非 pass/fail 单测）：
 *
 * <p>1. 按「构造即真值」生成 100 对中文提交对：{@code duplicate}（逐字复制 / 轻度改写，真雷同）与
 *    {@code distinct}（跨主题，非雷同），写入 {@code docs/evaluation/雷同标注集.json}；
 * 2. 用生产同款 {@link TextSimilarity#tfIdfCosine} 对每对打分（语料取该对两篇，等价于「两两比对」）；
 * 3. 对 0.45（进入相似列表）与 0.75（疑似雷同）两阈值算精确率 / 召回率 / F1，并扫描 F1 最优阈值。
 *
 * <p>运行：{@code mvn test -Dtest=SimilarityEvaluationTest}（不依赖 Docker/Redis，纯内存）。
 *
 * <p>局限（报告里也会说明）：标注集为合成数据，「非雷同」类全部取跨主题文本，区分度天然偏高；
 * 真实场景的难点是「同主题独立写作」，需用真实提交数据补充（见 docs/evaluation/真实数据采集步骤.md）。
 */
class SimilarityEvaluationTest {

    /** 构造真值用的话题语料：每话题 1 篇规范答案 + 2 篇不同强度的改写。 */
    record Topic(String name, String canonical, String v1, String v2) {
    }

    private static final List<Topic> TOPICS = List.of(
            new Topic("冒泡排序",
                    "冒泡排序通过相邻元素两两比较，把较大的数逐步交换到数组末尾，重复多轮直到整个数组有序。",
                    "冒泡排序通过相邻元素两两比较，把较大的数逐步交换到数组最后，重复多轮直到整个数组有序。",
                    "冒泡排序对相邻元素两两比较，把较大的数逐步换到数组末尾，重复多轮直到数组有序。"),
            new Topic("选择排序",
                    "选择排序每次从无序部分选出最小元素，放到已排序部分的末尾，重复直到全部有序。",
                    "选择排序每次从无序部分选出最小元素，放到已排序部分的最后，重复直到全部有序。",
                    "选择排序每轮从无序部分挑出最小元素，放到有序部分末尾，循环直到全部有序。"),
            new Topic("二分查找",
                    "二分查找在有序数组中每次比较中间元素，根据大小关系缩小一半查找范围，直到找到或区间为空。",
                    "二分查找在有序数组中每次比较中间元素，根据大小关系缩小一半查找范围，直到找到或区间空。",
                    "二分查找针对有序数组，每次和中间元素比较，按大小缩小一半范围，直到命中或区间为空。"),
            new Topic("顺序查找",
                    "顺序查找从数组第一个元素开始，逐个与目标值比较，找到即返回下标，遍历完未找到则返回不存在。",
                    "顺序查找从数组第一个元素开始，逐个和目标值比较，找到即返回下标，遍历完未找到则返回不存在。",
                    "顺序查找从数组首元素开始，逐个与目标值比较，命中就返回下标，扫完未找到则返回不存在。"),
            new Topic("栈",
                    "栈是一种后进先出的线性表，只允许在栈顶进行入栈和出栈操作。",
                    "栈是一种后进先出的线性表，只允许在栈顶进行压栈和弹栈操作。",
                    "栈是后进先出的线性表，只能在栈顶进行入栈和出栈操作。"),
            new Topic("队列",
                    "队列是一种先进先出的线性表，在队尾入队、在队头出队。",
                    "队列是一种先进先出的线性表，在队尾入队、在队头出列。",
                    "队列是先进先出的线性表，元素在队尾入队、在队头出队。"),
            new Topic("链表反转",
                    "链表反转通过遍历链表，把每个节点的指针指向前驱节点，最后返回新的头节点。",
                    "链表反转通过遍历链表，把每个结点的指针指向前驱结点，最后返回新的头结点。",
                    "反转链表通过遍历，把每个节点的指针指向前驱节点，最后返回新头节点。"),
            new Topic("斐波那契数列",
                    "斐波那契数列从第三项起每一项等于前两项之和，可用递归或循环实现。",
                    "斐波那契数列从第三项起每一项等于前两项的和，可用递归或循环实现。",
                    "斐波那契数列从第三项起每项等于前两项之和，可用递归或循环求解。"),
            new Topic("阶乘",
                    "n 的阶乘等于 1 到 n 所有整数的乘积，可用循环累乘或递归实现。",
                    "n 的阶乘等于 1 到 n 所有整数的乘积，可用循环累乘或递归编写。",
                    "阶乘 n 等于 1 到 n 所有整数相乘，可用循环累乘或递归实现。"),
            new Topic("最大公约数",
                    "求最大公约数常用辗转相除法，用较大数对较小数取余，反复直到余数为零。",
                    "求最大公约数常用辗转相除法，用较大数对较小数取余，反复直到余数是零。",
                    "求最大公约数常用辗转相除法，用大数对小数取余，反复直到余数为零。"),
            new Topic("回文判断",
                    "判断回文串可以用双指针，从两端向中间比较字符，全部相等则是回文。",
                    "判断回文串可以使用双指针，从两端向中间比较字符，全部相等则是回文。",
                    "判断回文可用双指针，从两端向中间比较字符，全部相同则是回文。"),
            new Topic("素数判断",
                    "判断素数可以试除到平方根，如果没有任何数整除则是素数。",
                    "判断素数可以试除到平方根，若没有任何数整除则是素数。",
                    "判断素数可试除到平方根，如果没有数整除则是素数。")
    );

    record Pair(int id, String a, String b, String label, String topic) {
    }

    record Scored(int id, String label, String topic, double score) {
    }

    /** 构造标注集：40 对真雷同（36 改写 + 4 逐字复制）+ 60 对跨主题非雷同。 */
    private static List<Pair> buildPairs() {
        List<Pair> pairs = new ArrayList<>();
        int id = 0;

        // 真雷同：规范答案 vs 两种强度改写，以及改写之间互比
        for (Topic t : TOPICS) {
            pairs.add(new Pair(++id, t.canonical, t.v1, "duplicate", t.name));
            pairs.add(new Pair(++id, t.canonical, t.v2, "duplicate", t.name));
            pairs.add(new Pair(++id, t.v1, t.v2, "duplicate", t.name));
        }
        // 真雷同：逐字复制（完全相同文本），覆盖「照抄」这一最容易判定的情形
        for (int i = 0; i < 4; i++) {
            Topic t = TOPICS.get(i);
            pairs.add(new Pair(++id, t.canonical, t.canonical, "duplicate", t.name + "（逐字复制）"));
        }

        // 非雷同：跨主题规范答案两两组合，取前 60 对（确定性顺序 i<j）
        int distinct = 0;
        outer:
        for (int i = 0; i < TOPICS.size(); i++) {
            for (int j = i + 1; j < TOPICS.size(); j++) {
                if (distinct >= 60) {
                    break outer;
                }
                pairs.add(new Pair(++id, TOPICS.get(i).canonical, TOPICS.get(j).canonical, "distinct",
                        "跨主题"));
                distinct++;
            }
        }
        return pairs;
    }

    @Test
    void evaluateSimilarityPrecisionRecall() throws Exception {
        List<Pair> pairs = buildPairs();

        // 1. 写标注集 JSON（交付物 docs/evaluation/雷同标注集.json）
        writeDataset(pairs);

        // 2. 逐对打分（语料 = 该对两篇，等价于两两比对）
        List<Scored> scored = new ArrayList<>();
        for (Pair p : pairs) {
            double s = TextSimilarity.tfIdfCosine(p.a, p.b, List.of(p.a, p.b));
            scored.add(new Scored(p.id, p.label, p.topic, s));
        }

        long dupCount = scored.stream().filter(s -> "duplicate".equals(s.label)).count();
        long disCount = scored.stream().filter(s -> "distinct".equals(s.label)).count();

        System.out.println("\n==================== 雷同检测离线评测结果 ====================");
        System.out.printf("标注集规模：%d 对（duplicate=%d, distinct=%d）%n", scored.size(), dupCount, disCount);
        for (double th : new double[]{0.45, 0.75}) {
            printMetrics(scored, th);
        }
        printBestThreshold(scored);
        printScoreDistribution(scored);

        // 3. 轻量正确性断言：真雷同平均分应显著高于非雷同（守护算法区分度，非评测通过标准）
        double dupAvg = avg(scored, "duplicate");
        double disAvg = avg(scored, "distinct");
        assertTrue(dupAvg > disAvg, "真雷同平均相似度应高于非雷同：" + dupAvg + " vs " + disAvg);
    }

    private static void printMetrics(List<Scored> scored, double th) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (Scored s : scored) {
            boolean isDup = "duplicate".equals(s.label);
            boolean positive = s.score >= th;
            if (isDup && positive) tp++;
            else if (isDup) fn++;
            else if (positive) fp++;
            else tn++;
        }
        double p = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double r = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double f1 = p + r == 0 ? 0 : 2 * p * r / (p + r);
        System.out.printf("%n阈值 %.2f：TP=%d FP=%d FN=%d TN=%d | 精确率=%.3f 召回率=%.3f F1=%.3f%n",
                th, tp, fp, fn, tn, p, r, f1);
    }

    private static void printBestThreshold(List<Scored> scored) {
        double bestTh = 0, bestF1 = -1;
        for (double th = 0.0; th <= 1.0001; th += 0.05) {
            double f1 = f1At(scored, th);
            if (f1 > bestF1) {
                bestF1 = f1;
                bestTh = th;
            }
        }
        System.out.printf("%nF1 最优阈值 ≈ %.2f（F1=%.3f）%n", bestTh, bestF1);
    }

    private static double f1At(List<Scored> scored, double th) {
        int tp = 0, fp = 0, fn = 0;
        for (Scored s : scored) {
            boolean isDup = "duplicate".equals(s.label);
            boolean positive = s.score >= th;
            if (isDup && positive) tp++;
            else if (isDup) fn++;
            else if (positive) fp++;
        }
        double p = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double r = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }

    private static void printScoreDistribution(List<Scored> scored) {
        System.out.println("\n相似度分布：");
        for (String label : List.of("duplicate", "distinct")) {
            double min = Double.MAX_VALUE, max = -1, sum = 0;
            int n = 0;
            for (Scored s : scored) {
                if (label.equals(s.label)) {
                    min = Math.min(min, s.score);
                    max = Math.max(max, s.score);
                    sum += s.score;
                    n++;
                }
            }
            System.out.printf("  %-9s n=%d  min=%.3f  max=%.3f  avg=%.3f%n",
                    label, n, min == Double.MAX_VALUE ? 0 : min, max, sum / n);
        }
        System.out.println("==============================================================");
    }

    private static double avg(List<Scored> scored, String label) {
        double sum = 0;
        int n = 0;
        for (Scored s : scored) {
            if (label.equals(s.label)) {
                sum += s.score;
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    private static void writeDataset(List<Pair> pairs) throws Exception {
        Path out = Paths.get(System.getProperty("user.dir"), "..", "docs", "evaluation")
                .toAbsolutePath().normalize();
        Files.createDirectories(out);

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("description", "雷同检测标注集：按构造规则标注真值（duplicate=真雷同/改写，distinct=跨主题非雷同）");
        meta.put("label_values", Map.of(
                "duplicate", "真雷同（逐字复制 / 轻度改写）",
                "distinct", "非雷同（跨主题，独立主题）"));
        meta.put("total", pairs.size());
        meta.put("limitation", "合成数据，非真实学生提交；'非雷同'类均为跨主题，区分度偏高。详见 雷同检测评测报告.md");
        root.put("meta", meta);

        List<Map<String, Object>> pairList = new ArrayList<>();
        for (Pair p : pairs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id);
            m.put("a", p.a);
            m.put("b", p.b);
            m.put("label", p.label);
            m.put("topic", p.topic);
            pairList.add(m);
        }
        root.put("pairs", pairList);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Path file = out.resolve("雷同标注集.json");
        mapper.writeValue(file.toFile(), root);
        System.out.println("[评测] 标注集已写入 " + file);
    }
}
