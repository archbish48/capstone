package com.community.demo.repository;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.community.Reaction;
import com.community.demo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostAndUser(Community post, User user);
    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);
}
