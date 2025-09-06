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
        boolean skip = uri.startsWith("/auth/")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || "/error".equals(uri);
        log.debug("[JWT] shouldNotFilter={} {} {}", skip, request.getMethod(), uri);
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String header = req.getHeader("Authorization");
        log.debug("[JWT] {} {} headerPresent={}", req.getMethod(), uri, header != null);

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                // 1) 토큰 파싱/검증
                Claims claims = jwtUtil.parseAccessToken(token); // 만료/서명 검증 포함
                String sub = claims.getSubject();                // 보통 userId
                if (sub == null) {
                    log.debug("[JWT] subject(null) in token");
                    chain.doFilter(req, res);
                    return;
                }

                Long userId;
                try {
                    userId = Long.parseLong(sub);
                } catch (NumberFormatException nfe) {
                    log.debug("[JWT] subject not a number: {}", sub);
                    chain.doFilter(req, res);
                    return;
                }

                // 2) 사용자 로드
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.debug("[JWT] user not found: {}", userId);
                    chain.doFilter(req, res);
                    return;
                }

                // 3) 권한 구성: 토큰의 role 클레임이 없으면 DB의 roleType 사용
                String claimRole = claims.get("role", String.class); // ex) "STUDENT"
                String roleName = (claimRole != null && !claimRole.isBlank())
                        ? claimRole
                        : (user.getRoleType() != null ? user.getRoleType().name() : "USER");

                String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

                var auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[JWT] setAuthentication userId={} authorities={}", user.getId(), auth.getAuthorities());
            }
        } catch (Exception e) {
            // 파싱/검증 실패 시 인증 미설정 상태로 통과 → Security가 401/403 판단
            log.debug("[JWT] token invalid on {} {} : {}", req.getMethod(), uri, e.getMessage());
        }

        chain.doFilter(req, res);
    }
}
