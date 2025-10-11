package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.registry.UserBusyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class UserBusyBlockInterceptor implements HandlerInterceptor {

    private final UserBusyRegistry busy;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User me)) return true; // 비로그인자는 통과(원하면 IP/세션 기준으로 확장)

        Long userId = me.getId();
        String path = req.getRequestURI();

        // 챗봇 엔드포인트는 서비스에서 락 검사(중복 호출은 423). 여기선 통과.
        if (path.startsWith("/chatbot/ask")) return true;

        // Swagger, docs, 정적 리소스, 헬스체크 등은 허용
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator") || path.equals("/favicon.ico")
                || path.startsWith("/error")) {
            return true;
        }

        // 같은 사용자가 챗봇 처리 중이면 다른 API 차단
        if (busy.isBusy(userId)) {
            res.setStatus(423); // Locked
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":\"USER_BUSY\",\"message\":\"Chat in progress. Please try again after it completes.\"}");
            return false;
        }
        return true;
    }
}