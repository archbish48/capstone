package com.community.demo.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationList {
    private Long notificationId;
    private Long noticeId;
    private String noticeTitle; //공지의 소속 학과
    private String department;
    private boolean read;
    private LocalDateTime createdAt;

    // 추가: 공지 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorProfileImageUrl;   // User.profileImageUrl

}
