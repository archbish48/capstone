package com.community.demo.dto.enroll;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MyBestResponse {
    private Long bestRecordMs;
    private BigDecimal bestRecordSeconds;
}