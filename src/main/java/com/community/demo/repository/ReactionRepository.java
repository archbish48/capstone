package com.community.demo.repository;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.community.Reaction;
import com.community.demo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostAndUser(Community post, User user);
    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);

    @Query("""
        SELECT r FROM Reaction r
        WHERE r.user = :user AND r.post IN :posts
        """)
    List<Reaction> findByUserAndPostIn(@Param("user") User user,
                                       @Param("posts") List<Community> posts);
}
