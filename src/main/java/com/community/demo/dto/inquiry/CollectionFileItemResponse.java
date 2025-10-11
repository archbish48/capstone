package com.community.demo.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CollectionFileItemResponse {
    private Long id;                 // 파일 id (다운로드시 사용할 예정)
    private String filename;         // originalFilename
    private LocalDateTime createdAt; // 업로드(저장) 일시
}