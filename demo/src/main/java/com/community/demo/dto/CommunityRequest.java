package com.community.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommunityRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "본문은 필수입니다.")
    private String text;
}
