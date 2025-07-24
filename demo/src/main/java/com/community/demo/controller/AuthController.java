package com.community.demo.controller;

import com.community.demo.dto.*;
import com.community.demo.service.LoginService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;

    // 이메일 인증번호 요청
    @PostMapping("/email/request")
    public ResponseEntity<String> requestAuthCode(@RequestBody EmailRequest request) {
        loginService.sendEmailAuthCode(request.getEmail());
        return ResponseEntity.ok("이메일로 인증코드가 전송되었습니다.");
    }

    // 이메일 인증번호 검증
    @PostMapping("/email/verify")
    public ResponseEntity<String> verifyAuthCode(@RequestBody VerifyRequest request) {
        loginService.verifyEmailAuthCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("이메일 인증 성공");
    }

    // 회원가입
    @PostMapping("/signup")
    public void signup(@RequestBody @Valid SignupRequest request){
        loginService.signup(request.getUsername(), request.getPassword(), request.getEmail(), request.getPhone(), request.getRole(), request.getDepartment());
    }

    // 로그인
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody @Valid LoginRequest request){
        return loginService.login(request.getUsername(), request.getPassword());
    }

    // 비밀번호 재설정 요청
    @PostMapping("/password/reset/request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody EmailRequest request) {
        loginService.sendEmailAuthCode(request.getEmail());  // 인증코드 전송
        return ResponseEntity.ok("비밀번호 재설정용 인증코드 전송 완료");
    }

    // 비밀번호 재설정 확인
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest request) {
        loginService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }


    //로그아웃
    @SecurityRequirement(name = "JWT")
    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authHeader){
        loginService.logout(authHeader);
    }


    @PostMapping("/reissue")
    public Map<String, String> reissue(@RequestBody Map<String, String> request){
        String refreshToken = request.get("refreshToken");
        return loginService.reissue(refreshToken);
    }


}

