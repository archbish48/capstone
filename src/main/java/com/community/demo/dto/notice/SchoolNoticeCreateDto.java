package com.community.demo.dto.notice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SchoolNoticeCreateDto {

    // 크롤링한 제목
    private String title;

    // 크롤링한 본문 (HTML 원본 또는 텍스트)
    private String text;

    // 학과 정보 (예: "컴퓨터공학과")
    private String department;
    

    // 원본 게시글의 작성시간
    private LocalDateTime originalCreatedAt;
}