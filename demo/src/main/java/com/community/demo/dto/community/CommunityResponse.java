package com.community.demo.dto.community;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CommunityResponse {

    private Long id;

    private String title;
    private String text;

    private String authorName;
    private String authorDepartment;
    private String authorRole;      //작성자의 권한

    private LocalDateTime updatedAt;

    private List<String> imageUrls;
    private List<String> tags;

    private int likeCount;
    private int dislikeCount;
    private int commentCount;

    private boolean bookmarked;

    private String myReaction;  // 내 reaction 상태를 알려주는 필드
}
