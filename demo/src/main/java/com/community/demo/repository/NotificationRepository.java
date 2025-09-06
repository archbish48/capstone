package com.community.demo.repository;

import com.community.demo.domain.notice.Notification;
import com.community.demo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 내 알림 전체(읽음/미읽음 모두) 최신순 페이지
    @EntityGraph(attributePaths = {"notice"}) // N+1 방지: Notice 함께 로딩
    Page<Notification> findByReceiverOrderByCreatedAtDesc(User receiver, Pageable pageable);

    // 선택된 알림 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiver = :receiver AND n.id IN :ids")
    int markAsReadByIds(@Param("receiver") User receiver, @Param("ids") List<Long> ids);

    // 선택된 알림 삭제
    void deleteByReceiverAndIdIn(User receiver, List<Long> ids);

    // 미읽음 알림 개수
    long countByReceiverAndReadFalse(User receiver);



}
