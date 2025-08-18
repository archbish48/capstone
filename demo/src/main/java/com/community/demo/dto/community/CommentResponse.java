package com.community.demo.dto.community;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long authorId;
    private String content;
    private String authorDept;        // 학과만 보여줌
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}