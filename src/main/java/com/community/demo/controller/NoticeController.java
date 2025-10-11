package com.community.demo.controller;


import com.community.demo.domain.user.User;
import com.community.demo.dto.notice.*;
import com.community.demo.service.notice.BookmarkService;
import com.community.demo.service.notice.NoticeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class NoticeController {

    private final NoticeService noticeService;
    private final BookmarkService bookmarkService;



    // 공지사항 목록 불러오기 (페이징, 썸네일 포함)
    @GetMapping
    public Page<NoticeListResponse> getNotices(
            @RequestParam(required = false) List<String> departments,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        bookmarkService.autoSubscribeToDepartmentManager(me);

        return noticeService.getFilteredNotices(departments, keyword, pageable, me);
    }

    // id로 notice 를 조회해 첨부 이미지 목록, 첨부파일 목록 추출, 작성자가 북마크 대상인지 확인 (공지사항 상세 보기 전용 로직)
    @GetMapping("/{id}")
    public NoticeResponse getNoticeDetail(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return noticeService.getNoticeDetail(id, me);
    }


    //  공지사항 작성 (파일 업로드 포함)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoticeResponse> create(
            @RequestPart("notice") String noticeJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachmentFiles) throws JsonProcessingException {

        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (imageFiles == null) imageFiles = List.of();
        if (attachmentFiles == null) attachmentFiles = List.of();

        // JSON 수동 파싱
        ObjectMapper mapper = new ObjectMapper();
        NoticeRequest noticeRequest = mapper.readValue(noticeJson, NoticeRequest.class);


        NoticeResponse created = noticeService.create(noticeRequest, me, imageFiles, attachmentFiles);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    //  공지사항 수정 (파일 포함, 덮어쓰기 방식)
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoticeResponse> update(
            @PathVariable Long id,
            @RequestPart("notice") String noticeJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImageFiles,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> newAttachmentFiles
    ) throws JsonProcessingException {

        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (newImageFiles == null) newImageFiles = List.of();
        if (newAttachmentFiles == null) newAttachmentFiles = List.of();

        ObjectMapper mapper = new ObjectMapper();
        NoticeUpdateRequest request = mapper.readValue(noticeJson, NoticeUpdateRequest.class);

        NoticeResponse updated = noticeService.update(id, request, me, newImageFiles, newAttachmentFiles);
        return ResponseEntity.ok(updated);
    }

    //  공지사항 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        noticeService.delete(id, me);
        return ResponseEntity.noContent().build();
    }

    // 구독 목록에서 내가 북마크한 작성자들의 공지사항을 최신순으로 6개씩 페이징해서 조회하는 API
    @GetMapping("/subscribed")
    public Page<NoticeListResponse> getSubscribedNotices(
            @RequestParam(required = false) List<String> departments,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return noticeService.getNoticesFromBookmarkedAuthors(me, departments, keyword, pageable);
    }

    // 특정 작성자의 공지사항을 최신순으로 6개씩 조회하는 API ( 작성자 클릭 시 해당 작성자의 공지 모아보기)
    @GetMapping("/author/{authorId}")
    public Page<NoticeListResponse> getNoticesByAuthor(
            @PathVariable Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")) // 최신순
        );

        return noticeService.getNoticesByAuthor(authorId, keyword, me, pageable);
    }

    // 내가 작성한 공지사항 전부 반환
    @GetMapping("/me")
    public Page<NoticeListResponse> getMyNotices(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return noticeService.getMyNotices(keyword, pageable, me);
    }



    // 첨부파일 다운로드 API ( GET /notices/download?filename=종합정보시간표.pdf) 등으로 다운로드 요청
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadAttachment(@RequestParam String filename) {
        try {
            Resource resource = noticeService.getAttachmentFile(filename);

            // 헤더에 들어갈 "표시용 파일명"은 베이스네임만 사용
            String displayName = Paths.get(filename).getFileName().toString();
            String encoded = UriUtils.encode(displayName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                    .body(resource);

        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 이미지 다운로드 API
    @GetMapping("/image-download")
    public ResponseEntity<Resource> downloadImage(@RequestParam String filename) {
        try {
            Resource resource = noticeService.getAttachmentFile(filename);

            // 이미지도 동일하게 베이스네임만 헤더에 사용
            String displayName = Paths.get(filename).getFileName().toString();
            String encoded = UriUtils.encode(displayName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // 강제 다운로드 유지
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                    .body(resource);

        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 이미지 미리보기 API (이미지 다운로드가 아니라 이미지를 페이지 안에서 렌더링해서 브라우저에서 바로 보여줌) GET /notices/image-preview?filename=IMAGE1.jpg
    @GetMapping("/image-preview")
    public ResponseEntity<Resource> previewImage(@RequestParam String filename) {
        try {
            Resource resource = noticeService.getAttachmentFile(filename);

            // 확장자 기반 MIME 추정: 논리경로여도 확장자만 보므로 정상 작동
            Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(filename);

            return ResponseEntity.ok()
                    .contentType(mediaType.orElse(MediaType.IMAGE_JPEG))
                    .body(resource);

        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }





}
