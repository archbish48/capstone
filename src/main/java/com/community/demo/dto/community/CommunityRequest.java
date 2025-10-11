package com.community.demo.dto.community;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CommunityRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "본문은 필수입니다.")
    private String text;

    private List<String> imageUrls;

    private List<String> tags;
}
