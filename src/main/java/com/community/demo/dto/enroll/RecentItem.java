package com.community.demo.dto.enroll;

import com.community.demo.domain.user.EnrollMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class RecentItem {
    private Instant finishedAt;
    private Long durationMs;
    private BigDecimal durationSeconds;
    private EnrollMode mode;
}
