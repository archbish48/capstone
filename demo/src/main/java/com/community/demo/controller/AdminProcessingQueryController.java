package com.community.demo.controller;

import com.community.demo.dto.inquiry.CollectionFilesGroupResponse;
import com.community.demo.dto.inquiry.CollectionNamesResponse;
import com.community.demo.service.user.AdminProcessingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/processing")
public class AdminProcessingQueryController {

    private final AdminProcessingQueryService queryService;

    // 1) 파일 목록 조회 (다중 리스트 구조: 컬렉션 → 파일들)
    @GetMapping("/files/tree")
    public ResponseEntity<List<CollectionFilesGroupResponse>> getFilesTree() {
        return ResponseEntity.ok(queryService.listFilesGrouped());
    }

    // 3) 폴더 이름 목록 조회
    @GetMapping("/collections")
    public ResponseEntity<CollectionNamesResponse> getCollections() {
        return ResponseEntity.ok(queryService.listCollectionNames());
    }
}