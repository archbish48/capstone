package com.community.demo.service.auth;

import com.community.demo.domain.auth.EmailAuthCode;
import com.community.demo.domain.user.RoleStatus;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.jwt.JwtUtil;
import com.community.demo.repository.EmailAuthCodeRepository;
import com.community.demo.repository.UserRepository;
import com.community.demo.security.PasswordUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailAuthCodeRepository emailAuthCodeRepository;
    private final EmailService emailService;


    // 메모리 기반 Refresh Token 저장소 (실제 서비스에선 DB 또는 Redis)
    private final Map<Long, String> refreshTokenStore = new ConcurrentHashMap<>();


    public LoginService(UserRepository userRepository, JwtUtil jwtUtil, EmailAuthCodeRepository emailAuthCodeRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.emailAuthCodeRepository = emailAuthCodeRepository;
        this.emailService = emailService;
    }

    private boolean isValidEmail(String email) {    //이메일 형식은 ~~@yiu.ac.kr 형식 고정
        //return email.contains("@");
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@yiu\\.ac\\.kr$");   //잠깐 취소(테스트용)
    }

    private boolean isValidPassword(String password) {
        // 대문자 1개 이상, 특수문자 1개 이상 포함하며 최소 8자 이상이어야 비밀번호 조건 완료
        return password.matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");
    }

    // 랜덤 6자리 숫자 생성 함수
    private String generate6DigitCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6자리 숫자 생성
        return String.valueOf(code);
    }

    // 회원가입용 이메일 인증 요청
    public void sendSignupAuthCode(String rawEmail) {       // string email 재개입된 변수 방지를 위해 변수 변경
        String normalizedEmail = rawEmail.toLowerCase().trim();         // trim 함수로 양쪽 공백 제거
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        sendOrUpdateCode(normalizedEmail);
    }

    // 비밀번호 재설정용 이메일 인증 요청
    public void sendPasswordResetAuthCode(String rawEmail) {
        String normalizedEmail = rawEmail.toLowerCase().trim();         // trim 함수로 양쪽 공백 제거
        if (userRepository.findByEmail(normalizedEmail).isEmpty()) {
            throw new IllegalArgumentException("가입되지 않은 이메일입니다.");
        }
        sendOrUpdateCode(normalizedEmail);
    }

    // 인증코드 전송 로직 (공통)
    private void sendOrUpdateCode(String email) {
        String code = generate6DigitCode();
        Optional<EmailAuthCode> existing = emailAuthCodeRepository.findTopByEmailOrderByIdDesc(email);

        if (existing.isPresent()) {
            EmailAuthCode auth = existing.get();
            auth.resetVerification(code);
            emailAuthCodeRepository.save(auth);
        } else {
            emailAuthCodeRepository.save(new EmailAuthCode(email, code));
        }

        emailService.sendAuthCode(email, code);
    }




    //이메일 인증번호 검증(회원가입, 비밀번호 찾기 시 사용)
    public void verifyEmailAuthCode(String email, String code){
        EmailAuthCode authCode = emailAuthCodeRepository
                .findByEmailAndCodeAndVerifiedFalse(email, code)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 인증코드입니다."));

        // 인증 처리
        authCode.markVerified();
        emailAuthCodeRepository.save(authCode);
    }


    // 회원가입
    public void signup(String username, String password, String email, String student_number, RoleType roleType, String department) {
        // 중복 검사
        // if(userRepository.findByUsername(username).isPresent()){throw new IllegalArgumentException("이미 존재하는 사용자입니다.");}
        if(userRepository.findByEmail(email).isPresent()) {throw new IllegalArgumentException("이미 등록된 이메일입니다.");}
        // 유효성 검사
        if(!isValidEmail(email)){ throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다.");}
        if(!isValidPassword(password)){throw new IllegalArgumentException("올바르지 않은 비밀번호 형식입니다.");}


        // 인증 여부 확인
        if (!emailAuthCodeRepository.existsByEmailAndVerifiedIsTrue(email)) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        User user = new User(username, hashedPassword, email, student_number, roleType, department);
        userRepository.save(user);
    }

    public ResponseEntity<?> login(String email, String password) {

        // 사용자를 찾고, 없을 경우 Optional로 받습니다.
        Optional<User> optionalUser = userRepository.findByEmail(email);

        // 사용자가 존재하지 않을 경우 404 Not Found 응답을 반환
        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND) // 404 Not Found 상태 코드
                    .body("사용자를 찾을 수 없습니다.");
        }

        User user = optionalUser.get();
        String hashedPassword = PasswordUtil.hashPassword(password);

        // 비밀번호가 일치하지 않을 경우 401 Unauthorized 응답을 반환
        if (!user.getPassword().equals(hashedPassword)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized 상태 코드
                    .body("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공 시 토큰과 함께 200 OK 응답을 반환
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoleType());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenStore.put(user.getId(), refreshToken);

        Map<String, String> tokens = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );

        return ResponseEntity.ok(tokens); // 200 OK
    }


    //로그아웃
    public void logout(String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 상태 아님");
        }

        String token = authHeader.substring(7); // substring 으로 token 에서 "Bearer " 제거
        Long userId = jwtUtil.validateAccessToken(token);
        refreshTokenStore.remove(userId);   //refresh Token 제거

    }
    public void resetPassword(String rawEmail, String newPassword) {
        String email = rawEmail.toLowerCase().trim();

        // 1. 인증 여부 확인
        if (!emailAuthCodeRepository.existsByEmailAndVerifiedIsTrue(email)) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 사용자 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 비밀번호 검증
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("비밀번호 형식이 올바르지 않습니다.");
        }

        // 4. 변경
        user.setPassword(PasswordUtil.hashPassword(newPassword));
        userRepository.save(user);
    }



    // 토큰 재발급 ( access token 유효 시간 만료 시 refresh token 을 이용해 재발급을 받음)
    public Map<String, String> reissue(String refreshToken) {
        Long userId = jwtUtil.validateRefreshToken(refreshToken);

        String savedRefreshToken = refreshTokenStore.get(userId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH 토큰이 일치하지 않음");
        }

        // userId 로부터 User 조회 (roleType 을 얻기 위해)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        // roleType 을 넣어서 accessToken 생성
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoleType());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        refreshTokenStore.remove(userId);  // 기존 토큰 제거
        refreshTokenStore.put(userId, newRefreshToken);  // 새 토큰 저장

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    
    //관리자 승인 API 예시버전. 로그인 컨트롤러에서 API 추가 필요, 로직 수정 더 필요
    @PatchMapping("/admin/approve/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user"));
        user.setRoleStatus(RoleStatus.ACTIVE);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

}

