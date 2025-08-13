package com.community.demo.service.notice;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {    // 파일 저장 구현체 클래스,로컬 저장으로 일단 정의해봤음


    @Value("${file.dir:uploads}")
    private String configuredDir;

    private Path rootDir;

    @PostConstruct
    public void init() throws IOException {
        Path candidate = Paths.get(configuredDir);
        if (!candidate.isAbsolute()) {
            ApplicationHome home = new ApplicationHome(LocalFileStorageService.class);
            candidate = home.getDir().toPath().resolve(candidate);
        }
        rootDir = candidate.toAbsolutePath().normalize();
        Files.createDirectories(rootDir);
        log.info("File storage root = {}", rootDir);

    }

    @Override
    public String save(MultipartFile file, String subDir) {
        try {
            String safeOriginal = sanitize(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "_" + safeOriginal;

            Path targetDir = (subDir == null || subDir.isBlank())
                    ? rootDir
                    : rootDir.resolve(subDir).normalize();

            if (!targetDir.startsWith(rootDir)) {
                throw new SecurityException("Invalid subDir outside root");
            }
            Files.createDirectories(targetDir);

            Path target = targetDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, REPLACE_EXISTING);

            // 저장된 '논리 경로'를 반환 (컨트롤러에서는 "/files/" + 논리경로 로 접근 가능)
            String logicalPath = (subDir == null || subDir.isBlank())
                    ? filename
                    : subDir.replace('\\', '/') + "/" + filename;

            return logicalPath;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    @Override
    public Resource loadAsResource(String storagePath) throws IOException {
        try {
            Path p = resolve(storagePath);
            if (!Files.exists(p)) throw new NoSuchFileException(storagePath);
            return new UrlResource(p.toUri());
        } catch (MalformedURLException ex) {
            throw new IOException("리소스 로딩 실패: " + storagePath, ex);
        }
    }

    @Override
    public Path resolve(String storagePath) {
        Path p = rootDir.resolve(storagePath).normalize();
        if (!p.startsWith(rootDir)) throw new SecurityException("Invalid path");
        return p;
    }

    @Override
    public Path getRootDir() {
        return rootDir;
    }

    private String sanitize(String original) {
        if (original == null) return "file";
        String base = Paths.get(original.replace("\\", "/")).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-가-힣\\s]", "_");
    }
}
