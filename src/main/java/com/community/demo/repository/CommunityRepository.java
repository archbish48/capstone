package com.community.demo.repository;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    @Modifying(flushAutomatically = true)
    @Query("update Community c set c.likeCount = c.likeCount + :delta where c.id = :postId")
    int bumpLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying(flushAutomatically = true)
    @Query("update Community c set c.dislikeCount = c.dislikeCount + :delta where c.id = :postId")
    int bumpDislikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    // 최신순
    @Query("""
        SELECT DISTINCT c FROM Community c
        LEFT JOIN c.tags t
        LEFT JOIN CommunityBookmark b ON b.post = c AND b.user = :bookmarkUser
        WHERE
            (:keyword IS NULL OR :keyword = '' 
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.text)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t)       LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:authorId IS NULL OR c.author.id = :authorId)
        AND (:onlyBookmarked = false OR b.id IS NOT NULL)
        ORDER BY c.createdAt DESC
        """)
    Page<Community> searchLatestFlexible(@Param("keyword") String keyword,
                                         @Param("authorId") Long authorId,              // 내 글 필터 (null 가능)
                                         @Param("bookmarkUser") User bookmarkUser,      // 북마크 필터용 유저 (null 가능)
                                         @Param("onlyBookmarked") boolean onlyBookmarked,
                                         Pageable pageable);

    // 인기순
    @Query("""
        SELECT DISTINCT c FROM Community c
        LEFT JOIN c.tags t
        LEFT JOIN CommunityBookmark b ON b.post = c AND b.user = :bookmarkUser
        WHERE
            (:keyword IS NULL OR :keyword = '' 
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.text)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t)       LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:authorId IS NULL OR c.author.id = :authorId)
        AND (:onlyBookmarked = false OR b.id IS NOT NULL)
        ORDER BY (c.likeCount - c.dislikeCount) DESC, c.createdAt DESC
        """)
    Page<Community> searchPopularFlexible(@Param("keyword") String keyword,
                                          @Param("authorId") Long authorId,
                                          @Param("bookmarkUser") User bookmarkUser,
                                          @Param("onlyBookmarked") boolean onlyBookmarked,
                                          Pageable pageable);
}
