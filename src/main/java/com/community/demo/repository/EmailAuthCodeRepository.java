package com.community.demo.repository;

import com.community.demo.domain.auth.EmailAuthCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailAuthCodeRepository extends JpaRepository<EmailAuthCode, Long> {
    // 이메일 기준 최신 요청 조회 (재발급 시 사용)
    Optional<EmailAuthCode> findTopByEmailOrderByIdDesc(String email);

    // 인증코드 검증
    Optional<EmailAuthCode> findByEmailAndCodeAndVerifiedFalse(String email, String code);

    // 인증 여부 확인
    boolean existsByEmailAndVerifiedIsTrue(String email);
}
