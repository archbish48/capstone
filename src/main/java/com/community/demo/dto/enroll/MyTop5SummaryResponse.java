package com.community.demo.dto.enroll;

import com.community.demo.domain.user.EnrollMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MyTop5SummaryResponse {
    private BigDecimal record1Seconds;  // 최신
    private BigDecimal record2Seconds;
    private BigDecimal record3Seconds;
    private BigDecimal record4Seconds;
    private BigDecimal record5Seconds;  // 가장 오래됨(최근 5개 중)
    private BigDecimal averageSeconds;  // 1~5 평균
    private EnrollMode mode;
}