package com.community.demo.service.notice;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {   // 파일 저장 인터페이스 정의
    String save(MultipartFile file);
}
