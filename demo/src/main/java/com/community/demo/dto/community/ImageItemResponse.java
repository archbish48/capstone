package com.community.demo.dto.community;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageItemResponse {
    private Long id;
    private String url;
}