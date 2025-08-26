package com.community.demo.service.user;

import com.community.demo.domain.user.EnrollMode;
import com.community.demo.domain.user.EnrollRecord;
import com.community.demo.domain.user.User;
import com.community.demo.dto.enroll.AverageByModeResponse;
import com.community.demo.dto.enroll.MyRecent5Response;
import com.community.demo.dto.enroll.MyTop5SummaryResponse;
import com.community.demo.dto.enroll.RecentItem;
import com.community.demo.repository.EnrollRecordRepository;
import com.community.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final EnrollRecordRepository recordRepository;

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



    // 종료 + 기록 저장 + 평균 차이 계산
    @Transactional
    public FinishCalc finishAndSave(User me, EnrollMode mode) {
        Instant startedAt = startTimeByUser.remove(me.getId());
        if (startedAt == null) {
            throw new ResponseStatusException(BAD_REQUEST, "시작하지 않았습니다. /start 먼저 호출하세요.");
        }
        Instant finishedAt = Instant.now();
        long durationMs = Math.max(0, finishedAt.toEpochMilli() - startedAt.toEpochMilli());

        if (durationMs > MAX_DURATION_MS) {
            throw new ResponseStatusException(BAD_REQUEST, "타이머가 만료되었습니다. 다시 시작해 주세요.");
        }

        // 1) 기록 저장
        EnrollRecord rec = new EnrollRecord();
        rec.setUser(me);
        rec.setMode(mode);
        rec.setStartedAt(startedAt);
        rec.setFinishedAt(finishedAt);
        rec.setDurationMs(durationMs);
        recordRepository.save(rec);

        // 2) (옵션) 유저 캐시 업데이트: 최근 5개 유지
        if (mode == EnrollMode.BASIC) me.pushRecentBasic(durationMs);
        else me.pushRecentCart(durationMs);
        userRepository.save(me);

        // 3) 타 유저 평균(동일 모드) 계산 → 차이(초)
        Double othersAvgMs = recordRepository.averageByModeExcludingUser(mode, me.getId());
        BigDecimal diffSeconds = null;
        BigDecimal measuredSeconds = toSeconds2f(durationMs);
        if (othersAvgMs != null) {
            BigDecimal othersAvgSec = toSeconds2f(othersAvgMs.longValue());
            // 내기록 - 타유저평균 (음수면 내가 빠름)
            diffSeconds = measuredSeconds.subtract(othersAvgSec).setScale(2, RoundingMode.HALF_UP);
        }

        return new FinishCalc(durationMs, measuredSeconds, diffSeconds, mode, finishedAt);
    }

    public void cancel(User me) {
        startTimeByUser.remove(me.getId());
    }

    // 내 최근 5개(모드별)
    public MyRecent5Response getMyRecent5(User me, EnrollMode mode) {
        var top5 = recordRepository.findTop5ByUserIdAndModeOrderByFinishedAtDesc(me.getId(), mode);
        var items = top5.stream().map(r -> new RecentItem(
                r.getFinishedAt(),
                r.getDurationMs(),
                toSeconds2f(r.getDurationMs()),
                r.getMode()
        )).toList();

        BigDecimal avg = items.isEmpty()
                ? null
                : items.stream()
                .map(RecentItem::getDurationSeconds)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);

        return new MyRecent5Response(items, avg);
    }

    // 전체 평균(모드별)
    public AverageByModeResponse getAverageByMode() {
        Double basicMs = recordRepository.averageByMode(EnrollMode.BASIC);
        Double cartMs  = recordRepository.averageByMode(EnrollMode.CART);
        return new AverageByModeResponse(
                basicMs == null ? null : toSeconds2f(basicMs.longValue()),
                cartMs  == null ? null : toSeconds2f(cartMs.longValue())
        );
    }

    // 내 1~5 기록 + 평균(모드별)
    public MyTop5SummaryResponse getMyTop5Summary(User me, EnrollMode mode) {
        var top5 = recordRepository.findTop5ByUserIdAndModeOrderByFinishedAtDesc(me.getId(), mode);
        // 최신순 1~5, 모자라면 null
        BigDecimal[] arr = new BigDecimal[5];
        for (int i = 0; i < 5; i++) {
            if (i < top5.size()) arr[i] = toSeconds2f(top5.get(i).getDurationMs());
            else arr[i] = null;
        }

        BigDecimal avg = null;
        long count = top5.size();
        if (count > 0) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < count; i++) sum = sum.add(arr[i]);
            avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }

        return new MyTop5SummaryResponse(arr[0], arr[1], arr[2], arr[3], arr[4], avg, mode);
    }

    // 공용 변환 함수
    public static BigDecimal toSeconds2f(long ms) {
        return BigDecimal.valueOf(ms / 1000.0).setScale(2, RoundingMode.HALF_UP);
    }

    // finish 계산 결과 전달용
    public record FinishCalc(long measuredMs,
                             BigDecimal measuredSeconds2f,
                             BigDecimal diffVsOthersSeconds2f,
                             EnrollMode mode,
                             Instant finishedAt) {}
}
