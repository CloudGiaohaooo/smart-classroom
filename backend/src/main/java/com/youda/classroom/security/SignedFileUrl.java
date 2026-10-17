package com.youda.classroom.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 文件下载签名 URL（Phase 20，复审 v3「本周」#9：/files/** 鉴权下载）。
 *
 * 前端下载文件用 window.open(fileUrl)，浏览器原生请求无法携带 JWT Authorization 头，
 * 因此不能简单地对 /files/** 要求登录。改用「签名 URL」：返回给前端的 fileUrl 附带
 * expires（过期时间戳，秒）与 sign（HMAC-SHA256 签名，覆盖路径），下载过滤器校验签名与时效，
 * 匿名 / 过期 / 篡改的直连下载一律拒绝，同时保持 window.open 可用。
 *
 * 签名密钥复用 jwt.secret（HS256 同一密钥，长度已满足 >= 32 字节），TTL 外置为
 * file.sign-url-ttl，不硬编码魔法值。
 */
@Component
public class SignedFileUrl {

    private final byte[] key;
    private final long ttlSeconds;

    public SignedFileUrl(@Value("${jwt.secret}") String secret,
                         @Value("${file.sign-url-ttl:1800}") long ttlSeconds) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    /** 为文件路径生成带有效期与签名的下载 URL（供前端 window.open / <a> 直接使用）。 */
    public String sign(String fileUrl) {
        long expires = System.currentTimeMillis() / 1000 + ttlSeconds;
        return fileUrl + "?expires=" + expires + "&sign=" + signInternal(fileUrl, expires);
    }

    /** 校验签名：路径未篡改 + 签名匹配 + 未过期。 */
    public boolean verify(String fileUrl, long expires, String sign) {
        if (sign == null || expires <= 0 || System.currentTimeMillis() / 1000 > expires) {
            return false;
        }
        String expected = signInternal(fileUrl, expires);
        // 常量时间比较，避免时序侧信道泄露签名
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8));
    }

    private String signInternal(String fileUrl, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal((fileUrl + "|" + expires).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("生成文件下载签名失败", e);
        }
    }
}
