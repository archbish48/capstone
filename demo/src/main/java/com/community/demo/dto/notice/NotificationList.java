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
    private String noticeTitle;
    private String department;
    private boolean read;
    private LocalDateTime createdAt;
}
