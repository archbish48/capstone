package com.community.demo.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class InquiryCreatedResponse {
    private Long pid;
    private String title;
    private String authorName;
    private String authorDepartment;
    private String authorProfileImageUrl;
    private LocalDateTime createdAt;
}