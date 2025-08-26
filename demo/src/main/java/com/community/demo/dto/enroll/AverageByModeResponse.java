package com.community.demo.dto.enroll;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class AverageByModeResponse {
    private BigDecimal basicAverageSeconds;   // 전체 BASIC 평균(초)
    private BigDecimal cartAverageSeconds;    // 전체 CART 평균(초)
}
