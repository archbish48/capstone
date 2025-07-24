package com.community.demo.repository;

import com.community.demo.domain.EmailAuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailAuthCodeRepository extends JpaRepository<EmailAuthCode, Long> {

    // 검증 조건: email, code 일치 + 아직 유효함 + 미확인
    @Query("SELECT a FROM EmailAuthCode a WHERE a.email = :email AND a.code = :code AND a.expiresAt > :now AND a.verified = false")
    Optional<EmailAuthCode> findValidCode(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);

    // 이메일 인증 여부 확인 (회원가입 전에 확인)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM EmailAuthCode a WHERE a.email = :email AND a.expiresAt > :now AND a.verified = true")
    boolean existsValidCodeVerified(@Param("email") String email, @Param("now") LocalDateTime now);
}
