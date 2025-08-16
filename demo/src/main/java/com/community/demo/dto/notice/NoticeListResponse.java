package com.community.demo.dto.notice;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeListResponse {
    private Long id;
    private String title;
    private String text;

    private Long authorId;
    private String authorName;
    private String authorRole;

    private LocalDateTime createdAt;   // 필요 없으면 추후 제거
    private LocalDateTime updatedAt;

    // 썸네일 필드 제거하고, 전체 리스트로 변경
    private List<String> imageUrls;       // 전체 이미지 URL 목록
    private List<String> attachmentUrls;  // 전체 첨부파일 URL 목록

    private boolean bookmarked;
}
