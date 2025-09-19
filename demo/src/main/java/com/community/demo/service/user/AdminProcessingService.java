package com.community.demo.service.user;

import com.community.demo.domain.user.BotFile;
import com.community.demo.domain.user.User;
import com.community.demo.dto.inquiry.BotFileUploadResponse;
import com.community.demo.dto.inquiry.FastApiBuildResult;
import com.community.demo.repository.BotFileRepository;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminProcessingService {

    private final BotFileRepository botFileRepository;

    // 로컬 저장 루트 (기존 LocalFileStorageService를 쓰고 있다면 그걸 주입해도 됩니다)
    @Value("${file.dir:uploads}")
    private String rootDir;

    // FastAPI 설정
    @Value("${app.fastapi.base-url:http://127.0.0.1:8000}")
    private String fastapiBaseUrl;

    @Value("${app.fastapi.processing-path:/api/v1/processing/process-pdf-full-and-build-db}")
    private String fastapiProcessPath;

    @Value("${app.fastapi.connect-timeout-ms:2000}")                           // ★
    private int connectTimeoutMs;

    @Value("${app.fastapi.read-timeout-ms:120000}")                             // ★
    private int readTimeoutMs;

    @Value("${app.fastapi.write-timeout-ms:120000}")
    private int writeTimeoutMs;


    @Value("${app.fastapi.auth.header-name:}")
    private String fastapiAuthHeaderName;

    @Value("${app.fastapi.auth.header-value:}")
    private String fastapiAuthHeaderValue;

    private WebClient webClient;

    private WebClient client() {
        if (webClient == null) {
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                    .responseTimeout(Duration.ofMillis(readTimeoutMs))
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS)));

            WebClient.Builder b = WebClient.builder()
                    .baseUrl(fastapiBaseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)); // 16MB

            // 선택: 인증 헤더 기본 세팅
            if (StringUtils.hasText(fastapiAuthHeaderName) && StringUtils.hasText(fastapiAuthHeaderValue)) {
                b.defaultHeader(fastapiAuthHeaderName, fastapiAuthHeaderValue);
            }

            webClient = b.build();
        }
        return webClient;
    }

    @Transactional
    public BotFileUploadResponse saveAndForward(User admin, String collectionName, MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("파일이 비어 있습니다.");
        if (!StringUtils.hasText(collectionName)) throw new IllegalArgumentException("collection_name이 비어 있습니다.");

        // 1) 서버 로컬에 **원본 파일명 그대로** 저장
        String originalFilename = file.getOriginalFilename();
        String contentType = (file.getContentType() != null) ? file.getContentType() : "application/pdf";
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = "file.pdf";
        }
        // 확장자 보정
        String ext = getExtension(originalFilename);
        if (!ext.equalsIgnoreCase(".pdf")) {
            // 업로드는 pdf만 허용(필요 시 제거 가능)
            throw new IllegalArgumentException("PDF 파일만 업로드할 수 있습니다.");
        }

        Path dir = Paths.get(rootDir, "botfiles", collectionName);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리를 만들 수 없습니다: " + dir, e);
        }

        Path savedPath = dir.resolve(originalFilename);
        try {
            // 동일 이름 존재 시 덮어쓸지/뒤에 (1) 붙일지 정책 선택
            // 여기서는 덮어쓰기 회피: (1), (2) ... 붙이기
            savedPath = avoidCollision(savedPath);
            Files.write(savedPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + savedPath, e);
        }

        // 2) DB 레코드 저장 (원본 파일명 그대로)
        BotFile bf = new BotFile();
        bf.setCollectionName(collectionName);
        bf.setOriginalFilename(originalFilename);
        bf.setStoredPath(savedPath.toString().replace('\\', '/'));
        bf.setSize(file.getSize());
        bf.setContentType(contentType);
        bf.setUploader(admin);

        BotFile saved = botFileRepository.save(bf);

        // 3) FastAPI로 전송 (한글 파일명 금지 → 안전 파일명으로 교체)
        String safeFilename = toAsciiSafeFilename(originalFilename);
        ByteArrayResource bodyFile = new NamedByteArrayResource(file, safeFilename);

        FastApiBuildResult fastapiResult;
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", bodyFile).contentType(MediaType.APPLICATION_PDF);

            // collection_name은 UTF-8 명시(한글 안전)
            builder.part("collection_name", collectionName)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");

            fastapiResult = client().post()
                    .uri(fastapiProcessPath)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().is2xxSuccessful()) {
                            return resp.bodyToMono(FastApiBuildResult.class);
                        } else {
                            // 실패 시 FastAPI의 에러 본문까지 읽어서 던짐
                            return resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> reactor.core.publisher.Mono.error(new RuntimeException(
                                            "FastAPI " + resp.statusCode().value() + " "
                                                    + resp.statusCode().value()
                                                    + " - body: " + body
                                    )));
                        }
                    })
                    .block();
        } catch (Exception ex) {
            // FastAPI 실패 시, DB/파일을 롤백하고 싶다면 여기서 예외를 던짐
            try { Files.deleteIfExists(savedPath); } catch (IOException ignored) {}
            throw new RuntimeException("FastAPI 전송/처리 실패: " + ex.getMessage(), ex);
        }

        return new BotFileUploadResponse(
                saved.getId(),
                saved.getCollectionName(),
                saved.getOriginalFilename(),
                saved.getStoredPath(),
                saved.getContentType(),
                saved.getSize(),
                saved.getCreatedAt(),
                fastapiResult
        );
    }

    // ---------- 유틸 ----------

    private static String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx >= 0) ? filename.substring(idx) : "";
    }

    private static Path avoidCollision(Path target) {
        if (!Files.exists(target)) return target;
        String name = target.getFileName().toString();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            base = name.substring(0, dot);
            ext  = name.substring(dot);
        }
        int i = 1;
        Path parent = target.getParent();
        while (true) {
            Path candidate = parent.resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(candidate)) return candidate;
            i++;
        }
    }

    // 파일명이 ASCII가 아니면 안전한 영문 이름으로 변경
    private static String toAsciiSafeFilename(String original) {
        if (isAscii(original)) return original;
        String ext = getExtension(original);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = randomAlphaNum(6);
        return "upload-" + timestamp + "-" + rand + (StringUtils.hasText(ext) ? ext.toLowerCase() : ".pdf");
    }

    private static boolean isAscii(String s) {
        return StandardCharsets.US_ASCII.newEncoder().canEncode(s);
    }

    private static String randomAlphaNum(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    // Multipart 전송 시 파일명을 강제로 지정하려면 Resource의 getFilename()을 오버라이드해야 함
    static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        NamedByteArrayResource(MultipartFile src, String filename) {
            super(toBytes(src));
            this.filename = filename;
        }
        @Override
        public String getFilename() {
            return filename;
        }
        private static byte[] toBytes(MultipartFile f) {
            try { return f.getBytes(); }
            catch (IOException e) { throw new RuntimeException(e); }
        }
    }
}