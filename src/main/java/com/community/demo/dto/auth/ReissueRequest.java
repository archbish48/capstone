package com.community.demo.dto.auth;

import lombok.Data;

@Data
public class ReissueRequest {
    private String refreshToken;
}