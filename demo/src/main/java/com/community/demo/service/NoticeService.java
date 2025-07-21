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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /* --------- 작성 --------- */
    @Transactional
    public NoticeResponse create(NoticeRequest dto, User author) {

        // MANAGER or ADMIN 검증
        if (author.getRoleType() == RoleType.STUDENT ||
                author.getRoleType() == RoleType.STAFF )
            throw new AccessDeniedException("작성 권한이 없습니다.");    // AccessDeniedException 은 뭐지?

        Notice n = new Notice();
        n.setTitle(dto.getTitle());
        n.setText(dto.getText());
        n.setDepartment(dto.getDepartment());
        n.setAuthor(author);

        noticeRepository.save(n);

        // 대상 학과 학생에게 알림 생성
        List<User> targets =
                userRepository.findByDepartmentAndRoleType(dto.getDepartment(), RoleType.STUDENT);
        targets.forEach(u -> notificationRepository.save(
                new Notification(null, u, n, false, null)));

        return toResponse(n);
    }

    /* --------- 읽기 --------- */
    @Transactional(readOnly = true)
    public List<NoticeResponse> listForDept(String dept) {
        return noticeRepository.findByDepartmentOrderByCreatedAtDesc(dept)
                .stream().map(this::toResponse).toList();
    }

    /* --------- 수정 / 삭제 --------- */
    @Transactional
    public NoticeResponse update(Long id, NoticeRequest dto, User me) {
        Notice n = findOr404(id);
        requireManagerOrAdmin(me);
        n.setTitle(dto.getTitle());
        n.setText(dto.getText());
        n.setDepartment(dto.getDepartment());
        return toResponse(n);
    }

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
    private void requireManagerOrAdmin(User u) {
        if (u.getRoleType() != RoleType.MANAGER
                && u.getRoleType() != RoleType.ADMIN)
            throw new AccessDeniedException("권한 없음");
    }
    private NoticeResponse toResponse(Notice n) {
        return new NoticeResponse(n.getId(), n.getTitle(), n.getText(),
                n.getDepartment(), n.getCreatedAt());
    }



}
