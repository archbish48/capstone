package com.community.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NoticeResponse {
    private Long id;
    private String title;
    private String text;
    private String department;
    private LocalDateTime createdAt;
}