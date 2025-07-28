package com.community.demo.service;

import com.community.demo.domain.notice.Attachment;
import com.community.demo.domain.notice.Notice;
import com.community.demo.domain.notice.NoticeImage;
import com.community.demo.domain.notice.Notification;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.notice.NoticeListResponse;
import com.community.demo.dto.notice.NoticeRequest;
import com.community.demo.dto.notice.NoticeResponse;
import com.community.demo.repository.NoticeRepository;
import com.community.demo.repository.NotificationRepository;
import com.community.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;


    //  목록 조회 - 페이징, 최신순, 썸네일 이미지 포함
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getNoticesForDepartment(String department, Pageable pageable) {
        return noticeRepository.findByDepartmentOrderByUpdatedAtDesc(department, pageable)
                .map(notice -> new NoticeListResponse(
                        notice.getId(),
                        notice.getUpdatedAt(),
                        notice.getImages().isEmpty() ? null : notice.getImages().get(0).getImageUrl()
                ));
    }

    //  상세 조회 - 전체 이미지 & 첨부파일 목록 포함
    @Transactional(readOnly = true)
    public NoticeResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다."));

        List<String> imageUrls = notice.getImages().stream()
                .map(NoticeImage::getImageUrl)
                .toList();

        List<String> attachmentUrls = notice.getAttachments().stream()
                .map(Attachment::getFileUrl)
                .toList();

        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getText(),
                notice.getDepartment(),
                notice.getUpdatedAt(),
                imageUrls,
                attachmentUrls
        );
    }



    //공지사항 작성
    @Transactional
    public NoticeResponse create(NoticeRequest dto, User user) {
        log.info("권한 체크 - 유저 RoleType: {}", user.getRoleType());
        // MANAGER or ADMIN 검증
        if (user.getRoleType() == RoleType.STUDENT ||
                user.getRoleType() == RoleType.STAFF )
            throw new AccessDeniedException("작성 권한이 없습니다.");    // AccessDeniedException 은 뭐지?

        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setText(dto.getText());
        notice.setDepartment(dto.getDepartment());
        notice.setAuthor(user);

        noticeRepository.save(notice);

        // 대상 학과 학생에게 알림 생성
        List<User> targets =
                userRepository.findByDepartmentAndRoleType(dto.getDepartment(), RoleType.STUDENT);
        targets.forEach(u -> notificationRepository.save(
                new Notification(null, u, notice, false, null)));

        return toResponse(notice);
    }

    // 읽기
    @Transactional(readOnly = true)
    public List<NoticeResponse> listForDept(String dept) {
        return noticeRepository.findByDepartmentOrderByCreatedAtDesc(dept)
                .stream().map(this::toResponse).toList();
    }

    // 수정
    @Transactional
    public NoticeResponse update(Long id, NoticeRequest dto, User user) {
        Notice n = findOr404(id);
        requireManagerOrAdmin(user);
        n.setTitle(dto.getTitle());
        n.setText(dto.getText());
        return toResponse(n);
    }

    //삭제
    @Transactional
    public void delete(Long id, User me) {
        Notice n = findOr404(id);
        requireManagerOrAdmin(me);
        noticeRepository.delete(n);
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
    private NoticeResponse toResponse(Notice notice) {  //기존 코드에서 첨부파일, 이미지, 그리고 createdAt을 없애고 이제는 updatedAt을 리턴하도록 변경
        List<String> imageUrls = notice.getImages().stream()
                .map(NoticeImage::getImageUrl)
                .toList();

        List<String> attachmentUrls = notice.getAttachments().stream()
                .map(Attachment::getFileUrl)
                .toList();
        
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getText(),
                notice.getDepartment(), notice.getUpdatedAt(), imageUrls, attachmentUrls);
    }



}
