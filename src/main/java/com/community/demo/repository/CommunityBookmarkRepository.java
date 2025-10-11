package com.community.demo.repository;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.community.CommunityBookmark;
import com.community.demo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityBookmarkRepository extends JpaRepository<CommunityBookmark, Long> {
    Optional<CommunityBookmark> findByUserAndPost(User user, Community post);

    boolean existsByUserAndPost(User user, Community post);

    List<CommunityBookmark> findByUser(User user);
}
