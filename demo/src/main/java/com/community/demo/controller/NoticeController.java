package com.community.demo.controller;


import com.community.demo.domain.User;
import com.community.demo.dto.NoticeRequest;
import com.community.demo.dto.NoticeResponse;
import com.community.demo.service.NoticeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.create(noticeRequest,me));
    }

    @PatchMapping("/{id}")
    public NoticeResponse update(@PathVariable Long id, @RequestBody @Valid NoticeRequest noticeRequest) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return noticeService.update(id,noticeRequest,me);
    }

    @DeleteMapping
    public ResponseEntity<NoticeResponse> delete(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        noticeService.delete(id, me);
        return ResponseEntity.noContent().build();
    }


}
