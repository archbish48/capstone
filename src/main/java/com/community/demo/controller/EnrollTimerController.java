package com.community.demo.controller;

import com.community.demo.domain.user.EnrollMode;
import com.community.demo.domain.user.User;
import com.community.demo.dto.enroll.*;
import com.community.demo.service.user.EnrollTimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;



@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@RequestMapping("/enroll-timer")
public class EnrollTimerController {

    private final EnrollTimerService timerService;

    // 타이머 시작 (기존과 동일)
    @Operation(summary = "타이머 시작", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/start")
    public ResponseEntity<StartResponse> start(@AuthenticationPrincipal User me) {
        var startedAt = timerService.start(me);
        return ResponseEntity.ok(new StartResponse(startedAt, "타이머를 시작했습니다."));
    }

    // 타이머 종료: 모드(BASIC/CART) 전달 → 기록 저장 + 평균 차이 계산
    @Operation(summary = "타이머 종료(기록 저장 + 평균차이)", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/finish")
    public ResponseEntity<FinishResponse> finish(@AuthenticationPrincipal User me,
                                                 @RequestBody FinishRequest req) {
        var res = timerService.finishAndSave(me, req.getMode());
        return ResponseEntity.ok(new FinishResponse(
                res.measuredMs(),
                res.measuredSeconds2f(),
                res.diffVsOthersSeconds2f(),   // (내기록 - 타유저평균), 음수면 더 빠름
                res.mode(),
                res.finishedAt()
        ));
    }

    // 최근 5개(모드별)
    @Operation(summary = "내 최근 5개 기록 조회(모드별)", security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me/recent")
    public ResponseEntity<MyRecent5Response> myRecent5(@AuthenticationPrincipal User me,
                                                       @RequestParam EnrollMode mode) {
        return ResponseEntity.ok(timerService.getMyRecent5(me, mode));
    }

    // 전체 평균(모드별)
    @Operation(summary = "전체 평균(모드별, 공개)", security = {})
    @GetMapping("/stats/average-by-mode")
    public ResponseEntity<AverageByModeResponse> averageByMode() {
        return ResponseEntity.ok(timerService.getAverageByMode());
    }

    // 내 1~5번째 기록 + 평균(모드별)
    @Operation(summary = "내 1~5번째 기록 + 평균(모드별)", security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me/summary")
    public ResponseEntity<MyTop5SummaryResponse> myTop5Summary(@AuthenticationPrincipal User me,
                                                               @RequestParam EnrollMode mode) {
        return ResponseEntity.ok(timerService.getMyTop5Summary(me, mode));
    }

    // (옵션) 취소
    @Operation(summary = "타이머 취소", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal User me) {
        timerService.cancel(me);
        return ResponseEntity.noContent().build();
    }


}
