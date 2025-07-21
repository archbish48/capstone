package com.community.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String text;

    @NotBlank
    private String department;   // 공지 대상 학과
}
