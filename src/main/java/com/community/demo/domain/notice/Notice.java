package com.community.demo.domain.notice;

import com.community.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notices")
public class Notice {       //공지사항 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "text")
    private String text;

    // 어느 학과용 공지인가
    private String department;                 // 예: "컴퓨터공학과"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)    // notices 와 notifications (알림저장용 테이블)은 참조 관계라 CASCADE 적용을 위해 코드 추가
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)        // ← attachments 는 fetch join 하지 말고 SUB SELECT 로
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoticeImage> images = new ArrayList<>();


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  void onCreate() { createdAt = LocalDateTime.now(); }
    @PreUpdate   void onUpdate() { updatedAt = LocalDateTime.now(); }
    public void removeAttachment(Attachment attachment) {
        attachments.remove(attachment);
        attachment.setNotice(null);
    }

    public void removeImage(NoticeImage image) {
        images.remove(image);
        image.setNotice(null);
    }
}
