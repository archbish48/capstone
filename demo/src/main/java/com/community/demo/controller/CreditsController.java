package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.service.user.CreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditsController {    //마이페이지 학점 정보 파일 업로드 및 수정 관련 컨트롤러

    private final CreditsService creditsService;

    /**
     * PDF 성적표 업로드 → FastAPI OCR 호출 → 응답 가공/저장 → 단일 JSON 반환
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file
    ) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> result = creditsService.forwardToOcrAndSave(me, file);
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자가 수정/입력한 학점 JSON을 DB에 반영
     * - 특히 "학점평점"과 각 항목의 "취득학점"을 반영
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateFromClient(
            @RequestBody Map<String, Object> editedPayload
    ) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        creditsService.applyEditedPayload(me, editedPayload);
        return ResponseEntity.noContent().build();
    }
}