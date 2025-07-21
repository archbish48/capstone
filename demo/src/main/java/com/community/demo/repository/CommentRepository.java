package com.community.demo.repository;

import com.community.demo.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List <Comment> findByPostIdOrderByCreatedAt(Long postId);
}
