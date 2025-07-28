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
    private String department;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    private List<String> attachmentUrls;
}