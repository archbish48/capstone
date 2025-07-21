package com.community.demo.jwt;

import com.community.demo.domain.User;
import com.community.demo.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.validateAccessToken(token);   // ← 서명·만료 검증
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {   // null-check 후에만 사용
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                                    user,                  // principal : 전체 객체 전달 추천
                                    null,
                                    Collections.emptyList()  // 권한 리스트
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    System.out.println("accessToken 수신: " + token);
                    System.out.println("사용자 ID 인증됨: " + user.getId());
                } else {
                    // DB에 계정이 없는 경우 401 오류
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account not found");
                    return;
                }

            } catch (JwtException | IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
