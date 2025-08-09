package com.community.demo.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class NoticeResponse {
    private Long id;
    private String title;
    private String text;

    private Long authorId;
    private String department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;     //이미지 url
    private List<String> attachmentUrls;    // 첨부파일
    private boolean isBookmarked; //  북마크 정보 추가
}