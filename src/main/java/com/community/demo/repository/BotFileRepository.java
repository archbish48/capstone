package com.community.demo.repository;

import com.community.demo.domain.user.BotFile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BotFileRepository extends JpaRepository<BotFile, Long> {

    // 컬렉션명만 distinct로 정렬해서 가져오기
    @Query("select distinct b.collectionName from BotFile b order by b.collectionName asc")
    List<String> findDistinctCollectionNames();

    // 정렬 조건으로 전체 가져오기 (서비스에서 그룹핑)
    List<BotFile> findAll(Sort sort);
}