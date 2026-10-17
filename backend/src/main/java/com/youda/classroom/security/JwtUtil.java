package com.youda.classroom.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类：负责 token 的生成与解析。
 * 采用 HS256 对称签名算法，密钥由配置文件注入。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-minutes}")
    private long expireMinutes;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token，把用户 id、角色作为自定义声明写入
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireMinutes * 60 * 1000L);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token，返回 Claims；解析失败（过期、被篡改、签名不匹配）会抛出异常
     */
    public Claims parseToken(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token);
        // 显式校验签名算法为 HS256，防御算法混淆攻击（纵深防御；jjwt 0.11 无 setAllowedAlgorithms 接口）
        if (!SignatureAlgorithm.HS256.getValue().equals(jws.getHeader().getAlgorithm())) {
            throw new JwtException("非预期的 JWT 签名算法");
        }
        return jws.getBody();
    }
}
