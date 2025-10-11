package com.community.demo.service.notice;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {   // 파일 저장 인터페이스 정의
    // subDir 예: "community/images", "notices/attachments" 등. null 또는 "" 이면 루트에 저장
    String save(MultipartFile file, String subDir);

    // 저장된 논리 경로(예: "community/images/uuid_name.jpg")로 Resource 로드
    Resource loadAsResource(String storagePath) throws IOException;

    // 논리 경로를 실제 파일 시스템 Path 로 해석
    Path resolve(String storagePath);

    // 정적 리소스 매핑 시 사용할 루트 디렉터리
    Path getRootDir();
}
