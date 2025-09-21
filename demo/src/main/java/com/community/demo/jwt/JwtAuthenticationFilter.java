package com.community.demo.jwt;

import com.community.demo.domain.user.User;
import com.community.demo.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        log.warn("[JWT] JwtAuthenticationFilter bean initialized: {}", this);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean skip =
                uri.startsWith("/auth/") ||
                        uri.startsWith("/v3/api-docs") ||
                        uri.startsWith("/swagger-ui") ||
                        uri.startsWith("/files/") ||            //  파일 공개 로직 추가
                        uri.startsWith("/profiles/") ||
                        "/error".equals(uri)
                        // 필요시 프리플라이트 옵선스는 통과
                        || "OPTIONS".equalsIgnoreCase(request.getMethod()); // [CHANGED] CORS preflight 스킵(선택)
        log.debug("[JWT] shouldNotFilter={} {} {}", skip, request.getMethod(), uri);
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String header = req.getHeader("Authorization");
        boolean hasBearer = header != null && header.startsWith("Bearer ");
        log.debug("[JWT] {} {} headerPresent={}", req.getMethod(), uri, header != null);

        try {
            // [CHANGED] 보호 경로인데 Bearer 토큰이 없으면 즉시 401로 종료 (기존 코드의 if 조건이 반대로 되어 있었음)
            if (!hasBearer) {
                reject(res, "invalid_token", "Missing Authorization: Bearer <token>");
                return;
            }

            // 여기 도달했으면 header는 null 아님
            String token = header.substring(7);

            // 1) 토큰 파싱/검증 (서명/만료/alg 등은 JwtUtil에서 검증)
            Claims claims = jwtUtil.parseAccessToken(token);

            // 2) subject(userId) 필수
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                reject(res, "invalid_token", "Missing subject"); // ★ 즉시 401
                return;
            }

            Long userId;
            try {
                userId = Long.parseLong(sub);
            } catch (NumberFormatException nfe) {
                reject(res, "invalid_token", "Invalid subject"); // ★ 즉시 401
                return;
            }

            // 3) 사용자 로드
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                reject(res, "invalid_token", "User not found"); // ★ 즉시 401
                return;
            }

            // 4) 권한 구성: DB 기준 (토큰의 role 클레임을 신뢰하지 않음)
            String roleName = (user.getRoleType() != null ? user.getRoleType().name() : "USER");
            String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

            var auth = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority(authority))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("[JWT] setAuthentication userId={} authorities={}", user.getId(), auth.getAuthorities());

            chain.doFilter(req, res);

            // ====== 예외 별로 401 처리하고 즉시 반환 ======
        } catch (io.jsonwebtoken.ExpiredJwtException e) {           // ★ 만료
            log.debug("[JWT] expired token on {} {} : {}", req.getMethod(), uri, e.getMessage());
            SecurityContextHolder.clearContext();
            reject(res, "invalid_token", "Token expired");
            return;

        } catch (io.jsonwebtoken.security.SignatureException e) {   // ★ 서명 불일치
            log.debug("[JWT] bad signature on {} {} : {}", req.getMethod(), uri, e.getMessage());
            SecurityContextHolder.clearContext();
            reject(res, "invalid_token", "Bad signature");
            return;

        } catch (io.jsonwebtoken.MalformedJwtException |
                 io.jsonwebtoken.UnsupportedJwtException e) {       // ★ 포맷/지원X
            log.debug("[JWT] malformed/unsupported token on {} {} : {}", req.getMethod(), uri, e.getMessage());
            SecurityContextHolder.clearContext();
            reject(res, "invalid_token", "Malformed or unsupported token");
            return;

        } catch (io.jsonwebtoken.JwtException e) {                  // ★ 기타 JJWT 예외(alg 위조 등)
            log.debug("[JWT] invalid token on {} {} : {}", req.getMethod(), uri, e.getMessage());
            SecurityContextHolder.clearContext();
            reject(res, "invalid_token", "Invalid token");
            return;

        } catch (Exception e) {                                     // ★ 그 외
            log.error("[JWT] unexpected error on {} {} ", req.getMethod(), uri, e);
            SecurityContextHolder.clearContext();
            reject(res, "invalid_request", "Authentication processing error");
            return;
        }
    }

    // RFC 6750 스타일 응답(프론트가 파싱하기 쉬움)
    private void reject(HttpServletResponse res, String error, String desc) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("WWW-Authenticate",
                "Bearer realm=\"api\", error=\"" + error + "\", error_description=\"" + desc + "\"");
        res.getWriter().write("{\"error\":\"" + error + "\",\"error_description\":\"" + desc + "\"}");
    }
}
