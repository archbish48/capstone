package com.community.demo.domain.notice;

import com.community.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification { // 알림 저장용 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    private Notice notice;

    @Column(name = "`read`")
    private boolean read = false;
    private LocalDateTime createdAt;

    public Notification(Object id, User user, Notice notice, boolean read, LocalDateTime createdAt) {
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(); }
}
