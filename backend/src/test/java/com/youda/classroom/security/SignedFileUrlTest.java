package com.youda.classroom.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件下载签名 URL 单测（Phase 20）：验证签名生成 / 校验 / 防篡改 / 过期拒绝。
 */
class SignedFileUrlTest {

    private static final String SECRET = "YouDaTestSecretKey-0123456789abcdefghijklmnopqrstuv";

    @Test
    void signAndVerifyRoundTrip() {
        SignedFileUrl s = new SignedFileUrl(SECRET, 1800);
        String url = s.sign("/files/1/abc.pdf");
        assertTrue(url.startsWith("/files/1/abc.pdf?expires="));

        String query = url.substring(url.indexOf('?') + 1);
        String[] parts = query.split("&");
        long expires = Long.parseLong(parts[0].substring("expires=".length()));
        String sign = parts[1].substring("sign=".length());

        assertTrue(s.verify("/files/1/abc.pdf", expires, sign));
    }

    @Test
    void verifyRejectsTamperedPath() {
        SignedFileUrl s = new SignedFileUrl(SECRET, 1800);
        String url = s.sign("/files/1/abc.pdf");
        String query = url.substring(url.indexOf('?') + 1);
        String[] parts = query.split("&");
        long expires = Long.parseLong(parts[0].substring("expires=".length()));
        String sign = parts[1].substring("sign=".length());

        // 拿课程 1 文件的签名去下载课程 2 的文件 → 拒绝（签名覆盖路径）
        assertFalse(s.verify("/files/2/xyz.pdf", expires, sign));
    }

    @Test
    void verifyRejectsExpiredOrMissingSign() {
        SignedFileUrl s = new SignedFileUrl(SECRET, 1800);
        long past = System.currentTimeMillis() / 1000 - 100;
        assertFalse(s.verify("/files/1/abc.pdf", past, "any"), "过期链接应拒绝");
        assertFalse(s.verify("/files/1/abc.pdf", System.currentTimeMillis() / 1000 + 100, null), "缺失签名应拒绝");
    }
}
