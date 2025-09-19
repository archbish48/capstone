package com.community.demo.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bot_files")
@Getter @Setter
@NoArgsConstructor
public class BotFile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String collectionName;        // 폴더명

    @Column(nullable = false, length = 255)
    private String originalFilename;      // **사용자 업로드 원본 파일명 (한글 그대로)**

    @Column(nullable = false, length = 500)
    private String storedPath;            // 서버 내 실제 저장 경로 (원본 파일명 그대로 저장)

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;                // 업로더 (ADMIN)

    @CreationTimestamp
    private LocalDateTime createdAt;
}