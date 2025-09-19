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

    // 선택된 알림 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiver = :receiver AND n.id IN :ids")
    int markAsReadByIds(@Param("receiver") User receiver, @Param("ids") List<Long> ids);

    // 선택된 알림 삭제
    void deleteByReceiverAndIdIn(User receiver, List<Long> ids);

    // 미읽음 알림 개수
    long countByReceiverAndReadFalse(User receiver);


    // 공지사항에서 상세조회 할 경우 알림창 리스트에서 똑같은 공지사항 삭제 처리
    @Modifying
    @Query("""
        delete from Notification n
         where n.receiver = :receiver
           and n.notice.id = :noticeId
    """)
    int deleteByReceiverAndNotice(@Param("receiver") User receiver,
                                  @Param("noticeId") Long noticeId);

    @EntityGraph(attributePaths = {"notice"})
    @Query("""
        select n
          from Notification n
         where n.receiver = :receiver
         order by n.read asc, n.createdAt desc
    """)
    Page<Notification> findByReceiverOrderUnreadFirst(@Param("receiver") User receiver, Pageable pageable);



    



}
