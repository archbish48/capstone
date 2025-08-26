package com.community.demo.dto.enroll;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class StartResponse {
    private Instant startedAt;          // 서버 기준 시작시각
    private String message;             // "타이머를 시작했습니다."
}
