package com.community.demo.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BotFileUploadResponse {
    private Long id;
    private String collectionName;
    private String originalFilename; // DB에는 원본 이름 유지
    private String storedPath;
    private String contentType;
    private long size;
    private LocalDateTime createdAt;

    private FastApiBuildResult fastapi;  // FastAPI 처리 결과
}