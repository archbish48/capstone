package com.community.demo.dto.bookmark;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkedAuthorDto {
    private Long authorId;
    private String authorName;
}