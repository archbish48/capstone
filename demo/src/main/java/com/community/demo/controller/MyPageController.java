package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.user.MyBriefProfileResponse;
import com.community.demo.dto.user.MyProfileResponse;
import com.community.demo.service.user.MyPageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class MyPageController {

    private final MyPageService myPageService;

    // 전체 필드 조회
    @GetMapping("/me")
    public MyProfileResponse getMyProfile() {
        User me = getCurrentUserOrThrow();
        return myPageService.getMyProfile(me.getId());
    }

    // 로그인 직후 헤더/사이드용 요약 정보
    @GetMapping("/brief")
    public MyBriefProfileResponse getMyBrief() {
        User me = getCurrentUserOrThrow();
        return myPageService.getMyBrief(me.getId());
    }

    private User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // User 엔티티로 저장 중인 패턴 유지
        return (User) auth.getPrincipal();
    }
}