package com.community.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
public class Reaction {     // reaction(좋아요, 싫어요) 테이블

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                     // 단일 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Community post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReactionType type;           // LIKE / DISLIKE

    public Reaction(Community post, User user, ReactionType type) {
        this.post = post;
        this.user = user;
        this.type = type;
    }
}


