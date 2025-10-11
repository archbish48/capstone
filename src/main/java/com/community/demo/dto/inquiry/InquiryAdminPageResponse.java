package com.community.demo.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InquiryAdminPageResponse<T> {
    private List<T> content;
    private int page;          // 현재 페이지(0-base)
    private int size;          // 페이지 크기
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}