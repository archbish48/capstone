package com.community.demo.repository;

import com.community.demo.domain.Community;
import com.community.demo.domain.Reaction;
import com.community.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostAndUser(Community post, User user);
}
