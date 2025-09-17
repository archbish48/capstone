package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.user.MyBriefProfileResponse;
import com.community.demo.dto.user.MyProfileResponse;
import com.community.demo.dto.user.UpdateMyInfoRequest;
import com.community.demo.service.user.MyPageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class MyPageController {

    private final MyPageService myPageService;
    private final ObjectMapper objectMapper;

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

    // 이름, 학과, 학번, 학년, 부전공 혹은 복수전공을 한번에 넘기는 api
    @GetMapping("/info")
    public Object getMyInfo() {
        User me = getCurrentUserOrThrow();
        return myPageService.getMyBasicInfo(me.getId());
    }


    // 내 정보 수정 (이름, 학과, 학번, "x학년 x학기", 부전공/복수전공, 프로필 이미지)
    @PatchMapping(value = "/info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object updateMyInfo(
            @RequestPart("payload") String payloadJson,                 // JSON 문자열
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        UpdateMyInfoRequest payload;
        try {
            payload = objectMapper.readValue(payloadJson, UpdateMyInfoRequest.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload JSON 파싱 실패: " + e.getMessage());
        }
        User me = getCurrentUserOrThrow();
        return myPageService.updateMyInfo(me.getId(), payload, profileImage); // profileImage가 null이면 이미지 미변경
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