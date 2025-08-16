package com.community.demo.service.notice;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

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
            String filename = lowerExt(safeOriginal);

            Path targetDir = (subDir == null || subDir.isBlank())
                    ? rootDir
                    : rootDir.resolve(subDir).normalize();

            if (!targetDir.startsWith(rootDir)) {
                throw new SecurityException("Invalid subDir outside root");
            }
            Files.createDirectories(targetDir);

            // 충돌 시 파일명 뒤에 (1), (2) 붙임
            String uniqueName = uniquify(targetDir, filename);
            Path target = targetDir.resolve(uniqueName).normalize();

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return (subDir == null || subDir.isBlank())
                    ? uniqueName
                    : subDir.replace('\\', '/') + "/" + uniqueName;

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    private String lowerExt(String name) {
        if (name == null) return "file";
        int dot = name.lastIndexOf('.');
        if (dot < 0) return name;
        return name.substring(0, dot) + name.substring(dot).toLowerCase(java.util.Locale.ROOT);
    }

    // 충돌 방지 버전(덮어쓰기 방지, 파일(1).jpg 식으로 저장)
    private String uniquify(Path dir, String filename) throws IOException {
        int dot = filename.lastIndexOf('.');
        String base = (dot < 0) ? filename : filename.substring(0, dot);
        String ext  = (dot < 0) ? ""       : filename.substring(dot); // 이미 소문자

        String candidate = filename;
        int i = 1;
        while (Files.exists(dir.resolve(candidate))) {
            candidate = base + "(" + i++ + ")" + ext;
        }
        return candidate;
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
