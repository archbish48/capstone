package com.community.demo.repository;

import com.community.demo.domain.notice.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findByDepartmentOrderByUpdatedAtDesc(String department, Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE n.author.id IN :authorIds ORDER BY n.updatedAt DESC")
    Page<Notice> findByAuthorIds(@Param("authorIds") Set<Long> authorIds, Pageable pageable);

    Page<Notice> findByAuthorIdOrderByUpdatedAtDesc(Long authorId, Pageable pageable);  // 작성자 ID 기준으로 페이징하며 최신순 정렬

    List<Notice> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);


    // 제목 또는 내용에 keyword 포함 여부 확인
    @Query("SELECT n FROM Notice n WHERE " +
            "(:departments IS NULL OR n.department IN :departments) AND " +
            "(:keyword IS NULL OR n.title LIKE %:keyword% OR n.text LIKE %:keyword%)")
    Page<Notice> findByDepartmentsAndKeyword(
            @Param("departments") List<String> departments,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 학과만
    Page<Notice> findByDepartmentIn(List<String> departments, Pageable pageable);

    // 검색어만
    @Query("SELECT n FROM Notice n WHERE n.title LIKE %:keyword% OR n.text LIKE %:keyword%")
    Page<Notice> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
