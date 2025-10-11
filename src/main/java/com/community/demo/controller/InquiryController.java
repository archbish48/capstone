package com.community.demo.controller;


import com.community.demo.domain.user.User;
import com.community.demo.dto.inquiry.InquiryCreateRequest;
import com.community.demo.dto.inquiry.InquiryCreatedResponse;
import com.community.demo.service.user.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<InquiryCreatedResponse> create(@RequestBody @Valid InquiryCreateRequest req) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User me)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        InquiryCreatedResponse res = inquiryService.create(me, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}