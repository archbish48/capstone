package com.community.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommunityResponse {
    private Long id;
    private String title;
    private String text;
}
