package com.community.demo.dto.enroll;

import com.community.demo.domain.user.EnrollMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class FinishResponse {
    private long measuredMs;
    private BigDecimal measuredSeconds;
    private BigDecimal diffVsOthersSeconds; // 내기록 - 타유저평균 (음수면 내가 더 빠름)
    private EnrollMode mode;
    private Instant finishedAt;
}