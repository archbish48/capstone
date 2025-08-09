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

}