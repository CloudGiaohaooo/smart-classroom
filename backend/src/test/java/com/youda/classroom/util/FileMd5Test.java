package com.youda.classroom.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文件 MD5 工具单测（创新点 C 核心算法）：
 * 校验 MD5 计算正确（对照 RFC 1321 标准用例）且输出为 32 位十六进制。
 */
class FileMd5Test {

    @Test
    void md5MatchesRfcStandardVectors() {
        // RFC 1321 附录 A.5 标准测试向量
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", FileMd5.md5Hex("".getBytes(StandardCharsets.UTF_8)), "空串");
        assertEquals("0cc175b9c0f1b6a831c399e269772661", FileMd5.md5Hex("a".getBytes(StandardCharsets.UTF_8)), "a");
        assertEquals("900150983cd24fb0d6963f7d28e17f72", FileMd5.md5Hex("abc".getBytes(StandardCharsets.UTF_8)), "abc");
    }

    @Test
    void md5IsDeterministicAndLowercase32Hex() {
        byte[] data = "优达智慧课堂-课程资源-秒传去重".getBytes(StandardCharsets.UTF_8);
        String md5 = FileMd5.md5Hex(data);
        assertEquals(32, md5.length(), "MD5 应为 32 位");
        assertEquals(md5, FileMd5.md5Hex(data), "相同内容 MD5 应一致");
        assertEquals(md5, md5.toLowerCase(), "应为小写十六进制");
    }
}
