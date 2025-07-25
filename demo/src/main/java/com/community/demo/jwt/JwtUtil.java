package com.community.demo.jwt;

import com.community.demo.domain.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final String accessSecret = "12345678901234567890123456789012";
    private final String refreshSecret = "abcdefghijabcdefghijabcdefghij12";
    private final long accessExpirationMs = 1000 * 60 * 15; // 15분 후 액세스 토큰 삭제
    private final long refreshExpirationMs = 1000L * 60 * 60 * 24 * 7; // 7일

    public String generateAccessToken(Long userId, RoleType roleType) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", "ROLE_" + roleType.name())  // 권한 정보 추가
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(Keys.hmacShaKeyFor(accessSecret.getBytes()))
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(Keys.hmacShaKeyFor(refreshSecret.getBytes()))
                .compact();
    }

    public Long validateAccessToken(String token) {
        return Long.parseLong(Jwts.parserBuilder()
                .setSigningKey(accessSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }

    public Long validateRefreshToken(String token) {
        return Long.parseLong(Jwts.parserBuilder()
                .setSigningKey(refreshSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
