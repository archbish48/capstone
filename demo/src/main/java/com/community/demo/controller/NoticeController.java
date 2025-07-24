package com.community.demo.controller;


import com.community.demo.domain.User;
import com.community.demo.dto.NoticeRequest;
import com.community.demo.dto.NoticeResponse;
import com.community.demo.service.NoticeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class NoticeController {

    private final NoticeService noticeService;


    //공지사항 불러오기
    @GetMapping
    public List<NoticeResponse> getNotices(@RequestParam String department) {
        return noticeService.listForDept(department);
    }

    //공지사항 작성
    @PostMapping
    public ResponseEntity<NoticeResponse> create(@RequestBody @Valid NoticeRequest noticeRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        log.info("Auth class: {}", auth.getPrincipal().getClass());
        log.info("Authorities: {}", auth.getAuthorities());

        if (auth.getPrincipal() instanceof User me) {
            log.info("User ID: {}, Role: {}, Dept: {}", me.getId(), me.getRoleType(), me.getDepartment());
            return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.create(noticeRequest, me));
        } else {
            log.error("Invalid principal: {}", auth.getPrincipal());
            throw new AccessDeniedException("사용자 정보가 유효하지 않습니다.");
        }
    }

    @PatchMapping("/{id}")
    public NoticeResponse update(@PathVariable Long id, @RequestBody @Valid NoticeRequest noticeRequest) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return noticeService.update(id,noticeRequest,me);   // noticeRequest dto 에는 department 가 포함되어 있지만 값을 넣어도 반영 X db 변화는 없음. dto 분리를 하기 까다로워서 일단 통합으로 사용중
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<NoticeResponse> delete(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        noticeService.delete(id, me);
        return ResponseEntity.noContent().build();
    }


}
