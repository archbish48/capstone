package com.community.demo.dto.bookmark;

import com.community.demo.domain.user.RoleType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkAuthorResponse {
    private Long authorId;
    private String authorName;
    private RoleType roleType;
    private boolean bookmarked;
}