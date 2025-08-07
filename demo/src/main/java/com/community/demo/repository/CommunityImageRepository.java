package com.community.demo.repository;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.community.CommunityImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityImageRepository extends JpaRepository<CommunityImage, Long> {
    void deleteByPost(Community post);
}
