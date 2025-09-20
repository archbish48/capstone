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
    private String authorName;
    private String department;
    private String authorProfileImageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<FileItemResponse> images;          // 수정을 위해 변경: id+url
    private List<FileItemResponse> attachments;     // 수정을 위해 변경: id+url

    private boolean bookmarked;
}