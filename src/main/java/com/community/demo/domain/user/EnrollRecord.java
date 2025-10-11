package com.community.demo.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "enroll_records",
        indexes = {
                @Index(name = "idx_enroll_user_mode_time", columnList = "user_id, mode, finishedAt")
        })
@Getter
@Setter
@NoArgsConstructor
public class EnrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnrollMode mode;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant finishedAt;

    @Column(nullable = false)
    private Long durationMs;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}