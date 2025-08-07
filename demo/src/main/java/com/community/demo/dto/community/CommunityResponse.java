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

    private LocalDateTime updatedAt;

    private List<String> imageUrls;
    private List<String> tags;

    private int likeCount;
    private int dislikeCount;
    private int commentCount;

    private boolean bookmarked;
}
