package com.smartpm.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTUtil {

    private static final String SECRET = "your-256-bit-secret-key-for-smartpm-project!!!";
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000L; // 24小时

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token */
    public static String generate(Long userId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRE_MS))
                .signWith(getKey())
                .compact();
    }

    /** 解析 token 中的所有 claims */
    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 token 中获取用户 ID */
    public static Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    /** 判断 token 是否过期 */
    public static boolean isExpired(String token) {
        try {
            parse(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /** 验证 token 是否合法 */
    public static boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
