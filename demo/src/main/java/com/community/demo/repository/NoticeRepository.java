package com.community.demo.repository;

import com.community.demo.domain.notice.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByDepartmentOrderByCreatedAtDesc(String dept);

    Page<Notice> findByDepartmentOrderByUpdatedAtDesc(String department, Pageable pageable);
}
