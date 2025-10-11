package com.community.demo.repository;

import com.community.demo.domain.notice.Bookmark;
import com.community.demo.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserAndAuthor(User user, User author);

    boolean existsByUserAndAuthor(User user, User author);


    @EntityGraph(attributePaths = "author")
    List<Bookmark> findAllByUser(User user);


    // 이 작성자(author)를 북마크(=구독)한 사용자들 반환
    @Query("select b.user from Bookmark b where b.author.id = :authorId")
    List<User> findSubscribersOfAuthor(Long authorId);

    // 필요하면 중복생성 방지/검사용
    boolean existsByUserIdAndAuthorId(Long userId, Long authorId);

}