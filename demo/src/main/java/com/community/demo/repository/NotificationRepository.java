package com.community.demo.repository;

import com.community.demo.domain.notice.Notification;
import com.community.demo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 미읽음 페이지 조회: 최신순. ManyToOne(fetch join) + countQuery 명시로 페이징 안전
    @Query(
            value = """
            select n from Notification n
            join fetch n.notice no
            join fetch no.author a
            where n.receiver = :receiver and n.read = false
            order by n.createdAt desc, n.id desc
        """,
            countQuery = """
            select count(n) from Notification n
            where n.receiver = :receiver and n.read = false
        """
    )
    Page<Notification> findUnreadByReceiverPaged(@Param("receiver") User receiver, Pageable pageable);

    // 조회된 그 페이지만 읽음 처리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.read = true
        where n.receiver = :receiver and n.id in :ids and n.read = false
    """)
    int markAsReadByIds(@Param("receiver") User receiver, @Param("ids") List<Long> ids);
}
