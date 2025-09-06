package com.community.demo.repository;

import com.community.demo.domain.notice.Notice;
import com.community.demo.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("""
    select distinct n from Notice n
    left join fetch n.author a
    left join fetch n.images i
    where n.id = :id
    """)
    Optional<Notice> findByIdWithDetails(@Param("id") Long id);

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


    @Query(
            value = """
            select n from Notice n
            where n.author.id = :authorId
              and ( :keyword is null or :keyword = '' 
                    or lower(n.title) like lower(concat('%', :keyword, '%'))
                    or lower(n.text)  like lower(concat('%', :keyword, '%')) )
            """,
            countQuery = """
            select count(n) from Notice n
            where n.author.id = :authorId
              and ( :keyword is null or :keyword = '' 
                    or lower(n.title) like lower(concat('%', :keyword, '%'))
                    or lower(n.text)  like lower(concat('%', :keyword, '%')) )
            """
    )
    Page<Notice> findMyNotices(@Param("authorId") Long authorId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    @Query(
            value = """
            select n from Notice n
            where n.author.id in :authorIds
              and ( :departments is null or n.department in :departments )
              and ( :keyword is null or :keyword = '' 
                    or lower(n.title) like lower(concat('%', :keyword, '%'))
                    or lower(n.text)  like lower(concat('%', :keyword, '%')) )
            """,
            countQuery = """
            select count(n) from Notice n
            where n.author.id in :authorIds
              and ( :departments is null or n.department in :departments )
              and ( :keyword is null or :keyword = '' 
                    or lower(n.title) like lower(concat('%', :keyword, '%'))
                    or lower(n.text)  like lower(concat('%', :keyword, '%')) )
            """
    )
    Page<Notice> findByBookmarkedAuthorsWithFilters(@Param("authorIds") Set<Long> authorIds,
                                                    @Param("departments") List<String> departments,
                                                    @Param("keyword") String keyword,
                                                    Pageable pageable);


    @Query(
            value = """
        select n from Notice n
        where n.author.id = :authorId
          and (
                :keyword is null or :keyword = '' or
                lower(n.title) like lower(concat('%', :keyword, '%')) or
                lower(n.text)  like lower(concat('%', :keyword, '%'))
              )
        """,
            countQuery = """
        select count(n) from Notice n
        where n.author.id = :authorId
          and (
                :keyword is null or :keyword = '' or
                lower(n.title) like lower(concat('%', :keyword, '%')) or
                lower(n.text)  like lower(concat('%', :keyword, '%'))
              )
        """
    )
    Page<Notice> findByAuthorIdAndKeyword(@Param("authorId") Long authorId,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);

}
