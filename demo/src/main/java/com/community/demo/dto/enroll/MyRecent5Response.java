package com.community.demo.dto.enroll;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyRecent5Response {
    private List<RecentItem> items;           // 최신순 최대 5개
    private BigDecimal myRecent5Average;      // 내 최근5 평균(초)
}
