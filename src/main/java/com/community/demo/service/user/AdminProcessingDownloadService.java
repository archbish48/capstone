package com.community.demo.service.user;

import com.community.demo.domain.user.BotFile;
import com.community.demo.repository.BotFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminProcessingDownloadService {

    private final BotFileRepository botFileRepository;

    @Value("${file.dir:uploads}")
    private String rootDir; // 업로드 루트: 예) uploads

    public ResponseEntity<Resource> downloadById(Long id) {
        BotFile f = botFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("파일이 존재하지 않습니다: id=" + id));

        Path path = Paths.get(f.getStoredPath());
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }

        Path root = Paths.get(rootDir).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new NoSuchElementException("파일을 찾을 수 없습니다: " + path);
        }

        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = (f.getContentType() != null) ? f.getContentType() : "application/pdf";
        }

        Resource resource = new FileSystemResource(path);

        HttpHeaders headers = new HttpHeaders();
        ContentDisposition cd = ContentDisposition
                .attachment()
                .filename(f.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(cd);
        headers.setCacheControl("private, max-age=0, must-revalidate");

        // 여기서는 contentLength를 지정하지 않고 그대로 리턴 (IOException 방지)
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}