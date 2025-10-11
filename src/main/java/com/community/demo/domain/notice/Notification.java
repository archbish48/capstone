package com.community.demo.domain.notice;

import com.community.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Notice notice;

    @Column(name = "`read`", nullable = false)
    private boolean read = false;

    private LocalDateTime createdAt;

    public Notification(User receiver, Notice notice) {
        this.receiver = receiver;
        this.notice = notice;
        this.read = false;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
