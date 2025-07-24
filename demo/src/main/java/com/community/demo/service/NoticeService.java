package com.community.demo.service;

import com.community.demo.domain.Notice;
import com.community.demo.domain.Notification;
import com.community.demo.domain.RoleType;
import com.community.demo.domain.User;
import com.community.demo.dto.NoticeRequest;
import com.community.demo.dto.NoticeResponse;
import com.community.demo.repository.NoticeRepository;
import com.community.demo.repository.NotificationRepository;
import com.community.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
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
    private NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getText(),
                notice.getDepartment(), notice.getCreatedAt());
    }



}
