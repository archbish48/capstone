package com.community.demo.repository;

import com.community.demo.domain.notice.Notification;
import com.community.demo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdAndReadFalse(Long userId);

    // 단순 파생 메서드(페이징 O, 정렬 고정)
    Page<Notification> findByReceiverOrderByCreatedAtDescIdDesc(User receiver, Pageable pageable);

    // 또는 fetch join으로 상세에 바로 쓸 거면 (권장)
    @Query("""
        select n from Notification n
        join fetch n.notice no
        join fetch no.author a
        where n.receiver = :receiver
        order by n.createdAt desc, n.id desc
    """)
    Page<Notification> findLatestByReceiver(@Param("receiver") User receiver, Pageable pageable);
}
