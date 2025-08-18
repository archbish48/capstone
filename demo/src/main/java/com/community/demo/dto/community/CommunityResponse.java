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

    private Long authorId;
    private String authorName;
    private String authorDepartment;
    private String authorRole;      //작성자의 권한

    // createdAt 도 넘겨주기
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 이미지 id + url 리스트
    private List<ImageItemResponse> images;

    private List<String> tags;

    private int likeCount;
    private int dislikeCount;
    private int commentCount;

    private boolean bookmarked;

    private String myReaction;  // 내 reaction 상태를 알려주는 필드
}
