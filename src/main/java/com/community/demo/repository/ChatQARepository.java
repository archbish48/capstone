package com.community.demo.repository;


import com.community.demo.domain.user.ChatQA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatQARepository extends JpaRepository<ChatQA, Long> {

    Page<ChatQA> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<ChatQA> findTop50ByUserIdOrderByCreatedAtDesc(Long userId); // 기본 50개

    Optional<ChatQA> findByIdAndUserId(Long id, Long userId);
}