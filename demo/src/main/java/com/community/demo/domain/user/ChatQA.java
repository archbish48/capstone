package com.community.demo.domain.user;

import com.community.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_qa",
        indexes = {
                @Index(name = "idx_chatqa_user_created", columnList = "user_id, created_at")
        }
)
@Getter @Setter @NoArgsConstructor
public class ChatQA {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text", nullable = false)
    private String question;

    @Column(columnDefinition = "text", nullable = false)
    private String answer;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ChatQA(User user, String question, String answer, Long latencyMs) {
        this.user = user;
        this.question = question;
        this.answer = answer;
        this.latencyMs = latencyMs;
    }
}