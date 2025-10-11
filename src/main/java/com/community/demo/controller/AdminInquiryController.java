package com.community.demo.controller;

import com.community.demo.dto.inquiry.InquiryAdminDeleteRequest;
import com.community.demo.dto.inquiry.InquiryAdminListItemResponse;
import com.community.demo.dto.inquiry.InquiryAdminPageResponse;
import com.community.demo.service.user.AdminInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/inquiries")
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    // 5) 문의 목록 조회 (페이지네이션, 기본 size=5, 최신순)
    @GetMapping
    public ResponseEntity<InquiryAdminPageResponse<InquiryAdminListItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(adminInquiryService.list(page, size));
    }

    // 6) 문의 삭제 (배치 삭제)
    @DeleteMapping
    public ResponseEntity<Void> deleteBatch(@RequestBody @Valid InquiryAdminDeleteRequest req) {
        adminInquiryService.deleteBatch(req);
        return ResponseEntity.noContent().build();
    }
}