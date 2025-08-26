package com.community.demo.dto.enroll;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AverageResponse {
    private BigDecimal averageBestSeconds; // 전체 유저의 최고기록 평균(초, 소수 둘째 자리)
}
