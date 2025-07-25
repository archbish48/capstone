package com.community.demo.dto;

import com.community.demo.domain.RoleType;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class SignupRequest {

    @NotBlank private String username;
    @NotBlank private String password;
    @Email private String email;
    @NotBlank String Student_number;
    @NotBlank private String department;

    private RoleType Role = RoleType.STUDENT;


}
