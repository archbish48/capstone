package com.community.demo.domain.community;


import com.community.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "community")
@NoArgsConstructor
public class Community {        //커뮤니티 테이블(좋아요, 싫어요 카운트변수는 이곳에 넣어뒀음 REACTION 에 없음)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    private String title;
    private String text;

    // 좋아요 싫어요 집계 칼럼
    private int likeCount;
    private int dislikeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;                // <- 작성자


    public Community(String tile, String text, User author) {
        this.title = tile;
        this.text = text;
        this.author = author;
    }
}
