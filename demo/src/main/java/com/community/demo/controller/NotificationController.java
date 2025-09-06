package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.notice.NotificationList;
import com.community.demo.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NoticeService noticeService;

    // 알림 목록 조회(조회 전용)
    @GetMapping("/me")
    public Page<NotificationList> getMyNotifications(
            @AuthenticationPrincipal User me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return noticeService.getMyNotifications(me, pageable);
    }

    // 선택 읽음 처리
    @PatchMapping("/me/read")
    public ResponseEntity<String> markAsRead(
            @AuthenticationPrincipal User me,
            @RequestBody List<Long> ids
    ) {
        int updated = noticeService.markAsRead(me, ids);
        return ResponseEntity.ok(updated + "건 읽음 처리 완료");
    }

    // 선택 삭제
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteNotifications(
            @AuthenticationPrincipal User me,
            @RequestBody List<Long> ids
    ) {
        noticeService.deleteNotifications(me, ids);
        return ResponseEntity.ok("삭제 완료");
    }

    // 미읽음 개수
    @GetMapping("/me/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User me) {
        long count = noticeService.getUnreadCount(me);
        return ResponseEntity.ok(count);
    }
}