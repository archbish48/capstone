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
//@JsonInclude(JsonInclude.Include.NON_NULL)    임시 NON NULL 제외
public class NoticeListResponse {
    private Long id;
    private String title;
    private String text;

    private Long authorId;
    private String authorName;
    private String authorDepartment;
    private String authorRole;

    private String authorProfileImageUrl;

    private LocalDateTime createdAt;   // 필요 없으면 추후 제거
    private LocalDateTime updatedAt;

    // 전체 이미지/첨부 리스트 (id+url)
    private List<FileItemResponse> images;
    private List<FileItemResponse> attachments;

    private boolean bookmarked;
}
