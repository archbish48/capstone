package com.community.demo.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component //  Spring Bean 등록
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${crawler.auth.key}") // 👈 1단계에서 정의한 키 값을 주입
    private String requiredApiKey;

    private static final String API_KEY_HEADER = "X-Auth-Token"; //  FastAPI 와 약속한 헤더 이름

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 API 키 추출
        String apiKey = request.getHeader(API_KEY_HEADER);

        // 2. 키 비교
        if (apiKey != null && apiKey.equals(requiredApiKey)) {
            // 3. 키가 유효하면, "인증된" 상태로 만들어줌
            // (이 요청은 'CRAWLER' 역할을 가졌다고 임시로 인증)
            var auth = new UsernamePasswordAuthenticationToken(
                    "crawler-service", // (Principal - 아무 문자열이나 상관없음)
                    null,              // (Credentials)
                    List.of(new SimpleGrantedAuthority("ROLE_CRAWLER")) // (Authorities)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 4. 다음 필터로 요청 전달
            filterChain.doFilter(request, response);
        } else {
            // 5. 키가 없거나 틀리면, 401 오류 반환
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"invalid_api_key\",\"error_description\":\"Missing or invalid X-Auth-Token header\"}");
        }
    }
}