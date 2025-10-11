package com.community.demo.controller;


import com.community.demo.domain.user.User;
import com.community.demo.dto.inquiry.BotFileUploadResponse;
import com.community.demo.service.user.AdminProcessingService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/processing")
public class AdminProcessingController {

    private final AdminProcessingService adminProcessingService;

    // 2) 챗봇 파일 추가 (동시: DB 저장 + FastAPI 전송)
    @PostMapping(value = "/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BotFileUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("collection_name") @NotBlank String collectionName
    ) {
        User admin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        BotFileUploadResponse res = adminProcessingService.saveAndForward(admin, collectionName, file);
        return ResponseEntity.ok(res);
    }
}