package com.community.demo.domain.user;


import com.community.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter @Setter
@NoArgsConstructor
public class Inquiry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // pid

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    // 작성 시점 스냅샷(알림리스트처럼 표시용)
    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(length = 100)
    private String authorDepartment;

    @Column(length = 500)
    private String authorProfileImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @CreationTimestamp
    private LocalDateTime createdAt;
}