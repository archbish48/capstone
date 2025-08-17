package com.community.demo.service.notice;

import com.community.demo.domain.notice.Attachment;
import com.community.demo.domain.notice.Notice;
import com.community.demo.domain.notice.NoticeImage;
import com.community.demo.domain.notice.Notification;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.notice.*;
import com.community.demo.repository.NoticeRepository;
import com.community.demo.repository.NotificationRepository;
import com.community.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FileStorageService fileStorageService;
    private final BookmarkService bookmarkService;

    private static final EnumSet<RoleType> WRITER_ROLES =
            EnumSet.of(RoleType.STAFF, RoleType.MANAGER, RoleType.ADMIN);

    private static final EnumSet<RoleType> EDIT_ANY_ROLES =
            EnumSet.of(RoleType.MANAGER, RoleType.ADMIN);

    // 작성 권한: STAFF/MANAGER/ADMIN
    private void requireWriterRole(User user) {
        if (user == null || user.getRoleType() == null || !WRITER_ROLES.contains(user.getRoleType())) {
            throw new AccessDeniedException("권한 없음");
        }
    }

    // 수정·삭제 권한: ADMIN/MANAGER 이면 무조건 허용, 아니면 작성자 본인만
    private boolean canModify(Notice notice, User user) {
        if (user == null || user.getRoleType() == null) return false;
        if (EDIT_ANY_ROLES.contains(user.getRoleType())) return true;
        return notice.getAuthor() != null && Objects.equals(notice.getAuthor().getId(), user.getId());
    }


    //  목록 조회 - 페이징, 최신순, 이미지, 첨부파일 포함 (공지사항 id, 제목, 내용, 작성자 이름, 작성자 역할, 날짜, 이미지, 첨부파일, 북마크 여부)
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getFilteredNotices(List<String> departments, String keyword, Pageable pageable, User user) {
        Set<Long> bookmarkedAuthors = bookmarkService.getBookmarkedAuthorIds(user);

        Page<Notice> notices;
        boolean hasDepartments = departments != null && !departments.isEmpty();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasDepartments && hasKeyword) {
            // 학과 + 검색어
            notices = noticeRepository.findByDepartmentsAndKeyword(departments, keyword, pageable);
        } else if (hasDepartments) {
            // 학과만
            notices = noticeRepository.findByDepartmentIn(departments, pageable);
        } else if (hasKeyword) {
            // 검색어만
            notices = noticeRepository.findByKeyword(keyword, pageable);
        } else {
            // 아무 필터 없음
            notices = noticeRepository.findAll(pageable);
        }

        return notices.map(notice -> {
            List<FileItemResponse> imageItems = notice.getImages().stream()
                    .map(img -> new FileItemResponse(img.getId(), img.getImageUrl()))
                    .toList();
            List<FileItemResponse> attachmentItems = notice.getAttachments().stream()
                    .map(att -> new FileItemResponse(att.getId(), att.getFileUrl()))
                    .toList();

            return new NoticeListResponse(
                    notice.getId(),
                    notice.getTitle(),
                    notice.getText(),
                    notice.getAuthor().getId(),
                    notice.getAuthor().getUsername(),
                    notice.getAuthor().getRoleType().name(),
                    notice.getCreatedAt(),
                    notice.getUpdatedAt(),
                    imageItems,
                    attachmentItems,       // ← 첨부 전체 리스트
                    bookmarkedAuthors.contains(notice.getAuthor().getId())
            );
        });
    }

    //  상세 조회 - 공지사항 id, 제목, 내용, 작성자 이름, 작성자 역할, 날짜, 이미지, 첨부파일, 북마크 여부
    @Transactional(readOnly = true)
    public NoticeResponse getNoticeDetail(Long noticeId, User user) {
        // (선택) N+1 줄이려면 @EntityGraph 로 images/attachments/author 를 함께 로딩하는 메서드로 교체
        Notice notice = noticeRepository.findByIdWithDetails(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다."));

        List<FileItemResponse> imageItems = notice.getImages().stream()
                .map(img -> new FileItemResponse(img.getId(), img.getImageUrl()))
                .toList();

        List<FileItemResponse> attachmentItems = notice.getAttachments().stream()
                .map(att -> new FileItemResponse(att.getId(), att.getFileUrl()))
                .toList();

        boolean isBookmarked = bookmarkService.isAuthorBookmarked(user, notice.getAuthor());

        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getText(),
                notice.getAuthor().getId(),
                notice.getDepartment(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                imageItems,            // ← id+url
                attachmentItems,       // ← id+url
                isBookmarked
        );
    }

    // 내가 북마크한 작성자들의 공지사항을 최신순으로 6개씩 페이징해서 조회
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getNoticesFromBookmarkedAuthors(
            User user, List<String> departments, String keyword, Pageable pageable) {

        Set<Long> bookmarkedAuthorIds = bookmarkService.getBookmarkedAuthorIds(user);
        if (bookmarkedAuthorIds.isEmpty()) return Page.empty(pageable);

        List<String> deptParam = (departments == null || departments.isEmpty()) ? null : departments;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;

        Page<Notice> page = noticeRepository.findByBookmarkedAuthorsWithFilters(
                bookmarkedAuthorIds, deptParam, kw, pageable);

        return page.map(notice -> {
            List<FileItemResponse> imageItems = notice.getImages().stream()
                    .map(img -> new FileItemResponse(img.getId(), img.getImageUrl()))
                    .toList();
            List<FileItemResponse> attachmentItems = notice.getAttachments().stream()
                    .map(att -> new FileItemResponse(att.getId(), att.getFileUrl()))
                    .toList();

            return new NoticeListResponse(
                    notice.getId(),
                    notice.getTitle(),
                    notice.getText(),
                    notice.getAuthor().getId(),
                    notice.getAuthor().getUsername(),
                    notice.getAuthor().getRoleType().name(),
                    notice.getCreatedAt(),
                    notice.getUpdatedAt(),
                    imageItems,
                    attachmentItems,
                    true
            );
        });
    }


    // 특정 작성자의 공지사항을 최신순으로 6개씩 조회하는 API
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getNoticesByAuthor(Long authorId, String keyword, User currentUser, Pageable pageable) {
        // keyword 정규화 (null/blank → null)
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;

        Page<Notice> notices = noticeRepository.findByAuthorIdAndKeyword(authorId, kw, pageable);
        Set<Long> bookmarkedAuthors = bookmarkService.getBookmarkedAuthorIds(currentUser);

        return notices.map(notice -> {
            List<FileItemResponse> imageItems = notice.getImages().stream()
                    .map(img -> new FileItemResponse(img.getId(), img.getImageUrl()))
                    .toList();
            List<FileItemResponse> attachmentItems = notice.getAttachments().stream()
                    .map(att -> new FileItemResponse(att.getId(), att.getFileUrl()))
                    .toList();

            return new NoticeListResponse(
                    notice.getId(),
                    notice.getTitle(),
                    notice.getText(),
                    notice.getAuthor().getId(),
                    notice.getAuthor().getUsername(),
                    notice.getAuthor().getRoleType().name(),
                    notice.getCreatedAt(),
                    notice.getUpdatedAt(),
                    imageItems,
                    attachmentItems,
                    bookmarkedAuthors.contains(notice.getAuthor().getId())
            );
        });
    }

    // 내가 작성한 공지사항 전체 리스트 조회 API ( 페이지 없이 전체 공지사항을 리스트로 반환)
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getMyNotices(String keyword, Pageable pageable, User user) {
        Page<Notice> page = noticeRepository.findMyNotices(user.getId(), keyword, pageable);

        return page.map(notice -> {
            List<FileItemResponse> imageItems = notice.getImages().stream()
                    .map(img -> new FileItemResponse(img.getId(), img.getImageUrl()))
                    .toList();
            List<FileItemResponse> attachmentItems = notice.getAttachments().stream()
                    .map(att -> new FileItemResponse(att.getId(), att.getFileUrl()))
                    .toList();

            return new NoticeListResponse(
                    notice.getId(),
                    notice.getTitle(),
                    notice.getText(),
                    notice.getAuthor().getId(),
                    notice.getAuthor().getUsername(),
                    notice.getAuthor().getRoleType().name(),
                    notice.getCreatedAt(),
                    notice.getUpdatedAt(),
                    imageItems,
                    attachmentItems,
                    false   // 내가 쓴 글이므로 북마크는 false
            );
        });
    }


    // 첨부파일, 이미지 다운로드 및 이미지 미리보기 API (다운로드, 프리뷰 공용)
    @Transactional(readOnly = true)
    public Resource getAttachmentFile(String filename) throws FileNotFoundException {
        // filename 에 "/files/..."가 넘어오면 접두어 제거해서 논리경로로 변환
        String storagePath = (filename != null && filename.startsWith("/files/"))
                ? filename.substring("/files/".length())
                : filename;

        try {
            return fileStorageService.loadAsResource(storagePath);
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("파일을 찾을 수 없습니다: " + filename);
        } catch (IOException e) {
            throw new RuntimeException("파일 로드 실패: " + filename, e);
        }
    }



    //공지사항 작성
    @Transactional
    public NoticeResponse create(NoticeRequest dto, User user,
                                 List<MultipartFile> imageFiles,
                                 List<MultipartFile> attachmentFiles) {

        requireWriterRole(user); // ← 변경: STAFF, MANAGER, ADMIN 통과

        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setText(dto.getText());
        notice.setDepartment(dto.getDepartment());
        notice.setAuthor(user);

        // 이미지 저장
        List<NoticeImage> images = (imageFiles == null ? List.<MultipartFile>of() : imageFiles).stream()
                .map(file -> {
                    String logicalPath = fileStorageService.save(file, "notices/images");
                    String url = "/files/" + logicalPath;
                    NoticeImage img = new NoticeImage();
                    img.setImageUrl(url);
                    img.setNotice(notice);
                    return img;
                })
                .toList();
        notice.setImages(images);

        // 첨부 저장
        List<Attachment> attachments = (attachmentFiles == null ? List.<MultipartFile>of() : attachmentFiles).stream()
                .map(file -> {
                    String logicalPath = fileStorageService.save(file, "notices/attachments");
                    String url = "/files/" + logicalPath;
                    Attachment att = new Attachment();
                    att.setFileUrl(url);
                    att.setNotice(notice);
                    return att;
                })
                .toList();
        notice.setAttachments(attachments);

        noticeRepository.save(notice);

        // 대상 학과 학생에게 알림
        List<User> targets = userRepository.findByDepartmentAndRoleType(dto.getDepartment(), RoleType.STUDENT);

        // 1) 단건씩 notice 저장
        targets.forEach(u -> notificationRepository.save(new Notification(u, notice)));

        return toResponse(notice);
    }


    @Transactional
    public NoticeResponse update(Long id, NoticeUpdateRequest dto, User user,
                                 List<MultipartFile> newImageFiles,
                                 List<MultipartFile> newAttachmentFiles) {

        Notice notice = findOr404(id);

        if (!canModify(notice, user)) { // MANAGER/ADMIN or 본인만 허용
            throw new AccessDeniedException("권한 없음");
        }

        if (dto.getTitle() != null)      notice.setTitle(dto.getTitle());
        if (dto.getText() != null)       notice.setText(dto.getText());
        if (dto.getDepartment() != null) notice.setDepartment(dto.getDepartment());

        // 1) 기존 이미지 중 '삭제 대상'만 제거
        Set<Long> removeImgIds = new HashSet<>(
                Optional.ofNullable(dto.getRemoveImageIds()).orElse(List.of()));
        if (!removeImgIds.isEmpty()) {
            // 컬렉션을 복사해 순회 중 동시 수정 이슈 방지
            List<NoticeImage> toRemove = notice.getImages().stream()
                    .filter(img -> img.getId() != null && removeImgIds.contains(img.getId()))
                    .toList();

            for (NoticeImage img : toRemove) {
                // (선택) 물리 파일 삭제: img.getImageUrl()에서 접두어("/files/" 또는 "/uploads/") 제거 후 resolve → Files.deleteIfExists
                // String logical = stripPublicPrefix(img.getImageUrl());
                // Files.deleteIfExists(fileStorageService.resolve(logical));
                notice.removeImage(img); // orphanRemoval=true 라면 DB에서도 삭제됨
            }
        }

        // 2) 기존 첨부 중 '삭제 대상'만 제거
        Set<Long> removeAttIds = new HashSet<>(
                Optional.ofNullable(dto.getRemoveAttachmentIds()).orElse(List.of()));
        if (!removeAttIds.isEmpty()) {
            List<Attachment> toRemove = notice.getAttachments().stream()
                    .filter(att -> att.getId() != null && removeAttIds.contains(att.getId()))
                    .toList();

            for (Attachment att : toRemove) {
                // (선택) 물리 파일 삭제
                // String logical = stripPublicPrefix(att.getFileUrl());
                // Files.deleteIfExists(fileStorageService.resolve(logical));
                notice.removeAttachment(att);
            }
        }

        // 3) 새 이미지 추가(있다면)
        if (newImageFiles != null) {
            for (MultipartFile file : newImageFiles) {
                if (file.isEmpty()) continue;
                String logicalPath = fileStorageService.save(file, "notices/images");
                String url = "/files/" + logicalPath; // 또는 "/uploads/"
                NoticeImage img = new NoticeImage();
                img.setNotice(notice);
                img.setImageUrl(url);
                notice.getImages().add(img);
            }
        }

        // 4) 새 첨부 추가(있다면)
        if (newAttachmentFiles != null) {
            for (MultipartFile file : newAttachmentFiles) {
                if (file.isEmpty()) continue;
                String logicalPath = fileStorageService.save(file, "notices/attachments");
                String url = "/files/" + logicalPath;
                Attachment att = new Attachment();
                att.setNotice(notice);
                att.setFileUrl(url);
                notice.getAttachments().add(att);
            }
        }

        // 영속 컨텍스트에 반영됨
        return toResponse(notice);
    }


    //삭제
    @Transactional
    public void delete(Long id, User me) {
        Notice n = findOr404(id);

        if (!canModify(n, me)) { // ← 변경
            throw new AccessDeniedException("권한 없음");
        }

        noticeRepository.delete(n);

        // 필요 시 실제 파일 삭제 로직을 추가 가능:
        // n.getImages() / n.getAttachments()의 url 에서 "/files/" 제거 → fileStorageService.resolve(논리경로)로 실제 Path 찾아 삭제
    }

    @Transactional // 조회 후 업데이트까지 한 트랜잭션에서 처리
    public Page<NotificationList> getMyUnreadPageAndMarkRead(User me, Pageable pageable, boolean markRead) {
        // 1) 미읽음 알림 "현재 페이지" 조회 (최신순은 쿼리에서 고정)
        Page<Notification> page = notificationRepository.findUnreadByReceiverPaged(me, pageable);

        // 2) DTO 매핑
        List<NotificationList> items = page.getContent().stream()
                .map(n -> new NotificationList(
                        n.getId(),
                        n.getNotice().getId(),
                        n.getNotice().getTitle(),
                        n.getNotice().getDepartment(),
                        n.isRead(),            // 여기서는 false일 것
                        n.getCreatedAt()
                ))
                .toList();

        // 3) 보여준 것만 읽음 처리
        if (markRead && !items.isEmpty()) {
            List<Long> ids = page.getContent().stream().map(Notification::getId).toList();
            notificationRepository.markAsReadByIds(me, ids);
        }

        // 4) 같은 페이지 메타로 반환 (totalElements는 업데이트 전 기준)
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }


    /* --------- 헬퍼 --------- */
    private Notice findOr404(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("notice"));
    }
    private void requireManagerOrAdmin(User user) {
        if (user.getRoleType() != RoleType.MANAGER
                && user.getRoleType() != RoleType.ADMIN)
            throw new AccessDeniedException("권한 없음");
    }
    private NoticeResponse toResponse(Notice notice) {
        List<FileItemResponse> imageItems = notice.getImages().stream()
                .map(img -> new FileItemResponse(img.getId(), img.getImageUrl()))
                .toList();

        List<FileItemResponse> attachmentItems = notice.getAttachments().stream()
                .map(att -> new FileItemResponse(att.getId(), att.getFileUrl()))
                .toList();

        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getText(),
                notice.getAuthor().getId(),
                notice.getDepartment(),
                notice.getCreatedAt(),      // NoticeResponse 에 createdAt 필드가 있다면 유지
                notice.getUpdatedAt(),
                imageItems,                 // ← id+url 리스트
                attachmentItems,            // ← id+url 리스트
                false                       // 생성/수정 응답이므로 북마크 표시는 false 고정
        );
    }


}
