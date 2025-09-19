package com.community.demo.dto.inquiry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class InquiryAdminListItemResponse {
    private Long pid;                     // 문의 id
    private String title;                 // 문의 제목
    private String content;               // 문의 내용 (ADMIN만 열람)
    private String authorName;            // 작성자 이름(스냅샷)
    private String authorDepartment;      // 작성자 학과(스냅샷)
    private String authorProfileImageUrl; // 프로필 이미지(스냅샷)
    private LocalDateTime createdAt;      // 작성일시
}