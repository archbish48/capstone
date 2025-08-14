package com.community.demo.repository;

import com.community.demo.domain.community.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    int countByPostId(Long postId);

    @EntityGraph(attributePaths = "author")         //N +1 EntityGraph 로 예방
    Page<Comment> findByPostId(Long postId, Pageable pageable);
}
