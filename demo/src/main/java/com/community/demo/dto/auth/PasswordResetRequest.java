package com.community.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordResetRequest {

    @Email
    private String email;

    @NotBlank
    private String code;

    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") //간단한 검증. 어차피 LoginService 에서 isValidPassword 로 검증 과정을 거치기 때문
    private String newPassword;
}
