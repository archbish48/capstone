package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.enroll.AverageResponse;
import com.community.demo.dto.enroll.FinishResponse;
import com.community.demo.dto.enroll.MyBestResponse;
import com.community.demo.dto.enroll.StartResponse;
import com.community.demo.service.user.EnrollTimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;



@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@RequestMapping("/enroll-timer")
public class EnrollTimerController {

    private final EnrollTimerService timerService;

    // 타이머 시작
    @Operation(summary = "타이머 시작", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/start")
    public ResponseEntity<StartResponse> start(@AuthenticationPrincipal User me) {
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        var startedAt = timerService.start(me);
        return ResponseEntity.ok(new StartResponse(startedAt, "타이머를 시작했습니다."));
    }

    // 타이머 종료 + 결과 반환 + 신기록 시 저장
    @PostMapping("/finish")
    public ResponseEntity<FinishResponse> finish() {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var result = timerService.finish(me);
        return ResponseEntity.ok(new FinishResponse(
                result.elapsedMs(),
                result.elapsedSeconds2f(),
                result.newRecord(),
                result.updatedBestMs(),
                result.updatedBestSeconds2f()
        ));
    }

    // 취소 버튼 누르면 측정 중이던 시간 cancel
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel() {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        timerService.cancel(me);
        return ResponseEntity.noContent().build();
    }

    // 내 최고기록 조회
    @GetMapping("/me/best")
    public ResponseEntity<MyBestResponse> myBest() {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long bestMs = timerService.getMyBestMs(me);
        return ResponseEntity.ok(new MyBestResponse(
                bestMs,
                (bestMs == null) ? null : EnrollTimerService.msToSeconds2f(bestMs)
        ));
    }

    // 전체 유저 최고기록 평균
    @GetMapping("/stats/average")
    public ResponseEntity<AverageResponse> averageBest() {
        var avgSec2f = timerService.getAverageBestSeconds2f();
        return ResponseEntity.ok(new AverageResponse(avgSec2f));
    }

    //테스트용 임시 api
//    @GetMapping("/debug/whoami")
//    public Map<String, Object> whoami(Authentication auth) {
//        return Map.of(
//                "authenticated", auth != null && auth.isAuthenticated(),
//                "principalClass", auth != null ? auth.getPrincipal().getClass().getName() : null,
//                "authorities", auth != null ? auth.getAuthorities() : null
//        );
//    }


}
