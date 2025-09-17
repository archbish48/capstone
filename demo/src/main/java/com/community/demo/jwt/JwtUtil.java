package com.community.demo.jwt;

import com.community.demo.domain.user.RoleType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // TODO: 실제 환경에선 application.yml에서 주입받고, 최소 32바이트(HS256) 이상 사용
    private final String accessSecret = "12345678901234567890123456789012";
    private final String refreshSecret = "abcdefghijabcdefghijabcdefghij12";

    private final long accessExpirationMs  = 1000L * 60 * 60;        // 60분
    private final long refreshExpirationMs = 1000L * 60 * 60 * 24 * 7; // 7일

    private SecretKey accessKey;
    private SecretKey refreshKey;

    // 기대 알고리즘을 고정(방어적)
    private static final String EXPECTED_ALG = SignatureAlgorithm.HS256.getValue(); // "HS256"

    @PostConstruct
    void init() {
        // 문자열 시크릿을 그대로 쓰더라도 Keys.hmacShaKeyFor 로 길이/형식 보장
        this.accessKey  = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));

        // 만약 Base64 인코딩 시크릿이라면 아래 방식 사용:
        // this.accessKey  = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        // this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
    }

    // 액세스 토큰 생성
    public String generateAccessToken(Long userId, RoleType roleType) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", "ROLE_" + roleType.name())   // 참고: 권한 결정은 서버 DB 기준 권장
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(accessKey, SignatureAlgorithm.HS256) // 알고리즘 명시
                .compact();
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(refreshKey, SignatureAlgorithm.HS256) // 알고리즘 명시
                .compact();
    }

    // 액세스 토큰 유효성 검사 후 userId 반환
    public Long validateAccessToken(String token) {
        Claims claims = parseAndVerify(token, accessKey);
        return Long.parseLong(claims.getSubject());
    }

    // 리프레시 토큰 유효성 검사 후 userId 반환
    public Long validateRefreshToken(String token) {
        Claims claims = parseAndVerify(token, refreshKey);
        return Long.parseLong(claims.getSubject());
    }

    // 액세스 토큰 Claims 반환(필터에서 사용)
    public Claims parseAccessToken(String token) {
        return parseAndVerify(token, accessKey);
    }

    // 필요시 리프레시 토큰 Claims 반환
    public Claims parseRefreshToken(String token) {
        return parseAndVerify(token, refreshKey);
    }

    // 공통: JWS(서명 포함)만 파싱 + 알고리즘 강제 확인
    private Claims parseAndVerify(String token, SecretKey key) {
        // (선제 차단 - 선택) 헤더에 alg:none 흔적이 보이면 즉시 거부
        rejectAlgNoneIfAny(token);

        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                // (선택) .requireIssuer("your-issuer")
                // (선택) .requireAudience("your-audience")
                .build()
                .parseClaimsJws(token); // JWS만 허용(서명 필수)

        String alg = jws.getHeader().getAlgorithm();
        if (!EXPECTED_ALG.equals(alg)) {
            // 기대한 알고리즘이 아니면 차단 (예: HS384/RS256 등)
            throw new JwtException("Unexpected alg: " + alg);
        }
        return jws.getBody();
    }

    //  아주 가벼운 선제 차단: alg:"none" 탐지
    private void rejectAlgNoneIfAny(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) throw new JwtException("Malformed JWT");
        String headerJson = new String(Decoders.BASE64URL.decode(parts[0]), StandardCharsets.UTF_8);
        if (headerJson.contains("\"alg\":\"none\"")) {
            throw new JwtException("Unsigned token rejected");
        }
    }

}
