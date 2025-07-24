package com.community.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailAuthCode {        //이메일 인증 관련 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String code;
    private LocalDateTime expiresAt;

    private boolean verified = false;   //인증 여부 저장

    public EmailAuthCode(String email, String code, LocalDateTime expiresAt ) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public void marKVerified(){
        this.verified = true;
    }

}
