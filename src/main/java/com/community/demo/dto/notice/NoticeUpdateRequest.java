package com.community.demo.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeUpdateRequest {
    private String title;
    private String text;
    private String department; // 부서도 수정 가능하면 유지, 아니면 제거해도 됨

    // 사용자가 '삭제'로 표시한 기존 파일들의 ID 목록
    private List<Long> removeImageIds;
    private List<Long> removeAttachmentIds;
}