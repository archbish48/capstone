package com.community.demo.dto.notice;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class NoticeListResponse {   //createdAt을 제거하고 updatedAt만 넘기도록 수정
    private Long id;
    private LocalDateTime updatedAt;
    private String thumbnailUrl;
}
