package com.youda.classroom.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件 MD5 工具（创新点 C：MD5 秒传去重）。
 * 纯 JDK 实现，不引入任何外部依赖。
 */
public final class FileMd5 {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private FileMd5() {
    }

    /**
     * 计算字节数组的 MD5 值，返回 32 位小写十六进制字符串。
     */
    public static String md5Hex(byte[] data) {
        try {
            return toHex(MessageDigest.getInstance("MD5").digest(data));
        } catch (NoSuchAlgorithmException e) {
            // JDK 17 默认必含 MD5，此分支仅作防御性处理
            throw new IllegalStateException("当前 JDK 不支持 MD5 算法", e);
        }
    }

    /**
     * 流式计算 MD5（Phase 20：流式上传）——边读边更新摘要，不把整个文件读入内存，
     * 避免大文件（100MB 上限）一次性 getBytes 造成的堆内存峰值。
     * 调用方负责关闭输入流。
     */
    public static String md5Hex(InputStream in) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 MD5 算法", e);
        }
    }

    private static String toHex(byte[] digest) {
        char[] out = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            out[i * 2] = HEX[(digest[i] >> 4) & 0xF];
            out[i * 2 + 1] = HEX[digest[i] & 0xF];
        }
        return new String(out);
    }
}
