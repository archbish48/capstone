package com.community.demo.repository;

import com.community.demo.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByDepartmentOrderByCreatedAtDesc(String dept);
}
