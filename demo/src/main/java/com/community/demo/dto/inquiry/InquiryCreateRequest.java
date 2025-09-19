package com.community.demo.dto.inquiry;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// FAQ DTO
@Getter @Setter
public class InquiryCreateRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}