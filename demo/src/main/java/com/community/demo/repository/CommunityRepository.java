package com.community.demo.repository;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    List<Community> findByAuthor(User user);
}
