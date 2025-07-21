package com.community.demo.service;

import com.community.demo.domain.RoleStatus;
import com.community.demo.domain.RoleType;
import com.community.demo.domain.User;
import com.community.demo.jwt.JwtUtil;
import com.community.demo.repository.UserRepository;
import com.community.demo.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 메모리 기반 Refresh Token 저장소 (실제 서비스에선 DB 또는 Redis)
    private final Map<Long, String> refreshTokenStore = new ConcurrentHashMap<>();

    public LoginService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    private boolean isValidEmail(String email) {    //email 형식은 @를 포함해야 함
        return email.contains("@");
    }

    private boolean isValidPassword(String password) {
        // 대문자 1개 이상, 특수문자 1개 이상 포함하며 최소 8자 이상이어야 비밀번호 조건 완료
        return password.matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");
    }

    public void signup(String username, String password, String email, String phone, RoleType roleType, String department) {
        // 중복 검사
        if(userRepository.findByUsername(username).isPresent()){throw new IllegalArgumentException("이미 존재하는 사용자입니다.");}
        if(userRepository.findByEmail(email).isPresent()) {throw new IllegalArgumentException("이미 등록된 이메일입니다.");}
        // 유효성 검사
        if(!isValidEmail(email)){ throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다.");}
        if(!isValidPassword(password)){throw new IllegalArgumentException("올바르지 않은 비밀번호 형식입니다.");}

        String hashedPassword = PasswordUtil.hashPassword(password);
        User user = new User(username, hashedPassword, email, phone, roleType, department);
        userRepository.save(user);
    }

    public Map<String, String> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String hashedPassword = PasswordUtil.hashPassword(password); // 비밀번호 해싱
        if(!user.getPassword().equals(hashedPassword)){ //해싱된 비밀번호가 일치하지 않는다면
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenStore.put(user.getId(), refreshToken);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }


    public void logout(String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 상태 아님");
        }

        String token = authHeader.substring(7); // substring 으로 token 에서 "Bearer " 제거
        Long userId = jwtUtil.validateAccessToken(token);

        refreshTokenStore.remove(userId);   //refresh Token 제거

    }

    public Map<String, String> reissue(String refreshToken) {
        Long userId = jwtUtil.validateRefreshToken(refreshToken);

        String savedRefreshToken = refreshTokenStore.get(userId);
        if(savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH 토큰이 일치하지 않음");
        }

        refreshTokenStore.remove(userId);
        String newAccessToken = jwtUtil.generateAccessToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        refreshTokenStore.put(userId, newRefreshToken);

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

