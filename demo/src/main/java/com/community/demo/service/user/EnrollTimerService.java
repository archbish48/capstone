package com.community.demo.service.user;

import com.community.demo.domain.user.User;
import com.community.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollTimerService {

    private final UserRepository userRepository;


    // 유저별 타이머 시작시각(서버 메모리 보관)
    private final Map<Long, Instant> startTimeByUser = new ConcurrentHashMap<>();

    // 예: 최대 30초만 유효
    private static final long MAX_DURATION_MS = 30_000L;

    // 타이머 시작: 기존 시작값이 있으면 덮어씀
    public Instant start(User me) {
        Instant now = Instant.now();
        startTimeByUser.put(me.getId(), now); // 이전 시작시각은 덮어씀
        return now;
    }

    // 타이머 종료: 경과시간 계산 후 신기록이면 User 갱신/저장
    public FinishResult finish(User me) {
        Instant startedAt = startTimeByUser.remove(me.getId());
        if (startedAt == null) {
            throw new ResponseStatusException(BAD_REQUEST, "시작하지 않은 타이머입니다. 먼저 /start를 호출하세요.");
        }

        long durationMs = Math.max(0, Instant.now().toEpochMilli() - startedAt.toEpochMilli());

        //  오래 방치된 타이머는 만료 처리 (현재 기준 30초 이상)
        if (durationMs > MAX_DURATION_MS) {
            throw new ResponseStatusException(BAD_REQUEST, "타이머가 만료되었습니다. 다시 시작해 주세요.");
        }

        boolean isNew = me.updateBestIfBetter(durationMs);
        if (isNew) userRepository.save(me);
        return new FinishResult(durationMs, isNew, me.getBestEnrollRecordMs());
    }

    // 사용자가 취소 버튼 눌렀을 때
    public void cancel(User me) {
        startTimeByUser.remove(me.getId());
    }

    // 내 최고기록 조회(ms), 없으면 null
    public Long getMyBestMs(User me) {
        return me.getBestEnrollRecordMs();
    }

    // 전체 유저 최고기록 평균(초, 소수 둘째 자리 반올림). 기록이 아무도 없으면 null */
    public BigDecimal getAverageBestSeconds2f() {
        Double avgMs = userRepository.averageBestRecordMs();
        if (avgMs == null) return null;
        BigDecimal sec = BigDecimal.valueOf(avgMs / 1000.0);
        return sec.setScale(2, RoundingMode.HALF_UP);
    }

    // 헬퍼: 밀리초 → 초(소수 둘째 자리 반올림)
    public static BigDecimal msToSeconds2f(long ms) {
        return BigDecimal.valueOf(ms / 1000.0).setScale(2, RoundingMode.HALF_UP);
    }

    // 종료 결과 DTO(서비스 내부용)
    public record FinishResult(long elapsedMs, boolean newRecord, Long updatedBestMs) {
        public BigDecimal elapsedSeconds2f() { return msToSeconds2f(elapsedMs); }
        public BigDecimal updatedBestSeconds2f() {
            return (updatedBestMs == null) ? null : msToSeconds2f(updatedBestMs);
        }
    }
}
