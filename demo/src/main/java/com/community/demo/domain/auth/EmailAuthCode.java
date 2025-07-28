package com.community.demo.domain.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailAuthCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String code;

    private boolean verified = false;

    public EmailAuthCode(String email, String code) {
        this.email = email;
        this.code = code;
        this.verified = false;
    }

    public void markVerified() {
        this.verified = true;
    }

    public void resetVerification(String newCode) {
        this.code = newCode;
        this.verified = false;
    }
}

