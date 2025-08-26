package com.community.demo.dto.enroll;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class FinishResponse {
    private long elapsedMs;                  // 2830
    private BigDecimal elapsedSeconds;       // 2.83
    private boolean newRecord;               // 신기록 여부
    private Long bestRecordMs;               // 서버 저장된 내 최고기록(ms)
    private BigDecimal bestRecordSeconds;    // 초 단위(소수 둘째 자리)
}