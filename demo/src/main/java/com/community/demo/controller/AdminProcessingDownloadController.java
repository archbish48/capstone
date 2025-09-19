package com.community.demo.controller;


import com.community.demo.service.user.AdminProcessingDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/processing")
public class AdminProcessingDownloadController {

    private final AdminProcessingDownloadService downloadService;

    // 4) 파일 다운로드 API
    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return downloadService.downloadById(id);
    }
}