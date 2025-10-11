package com.community.demo.dto.community;

import lombok.Data;

import java.util.List;

@Data
public class CommunityUpdateRequest {
    private String title;                 // null 아니면 수정
    private String text;                  // null 아니면 수정
    private List<String> tags;            // null 아니면 수정 (빈 리스트도 허용하면 전부 제거 의미)

    private List<Long> removeImageIds;    // 지울 이미지 id 목록 (없으면 삭제 없음)
}