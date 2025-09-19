package com.community.demo.service.user;

import com.community.demo.domain.user.BotFile;
import com.community.demo.dto.inquiry.CollectionFileItemResponse;
import com.community.demo.dto.inquiry.CollectionFilesGroupResponse;
import com.community.demo.dto.inquiry.CollectionNamesResponse;
import com.community.demo.repository.BotFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminProcessingQueryService {

    private final BotFileRepository botFileRepository;

    /**
     * 파일 목록(그룹핑) 조회
     * - 컬렉션명 오름차순
     * - 동일 컬렉션 내 파일은 최신순(작성일시 DESC)
     */
    @Transactional(readOnly = true)
    public List<CollectionFilesGroupResponse> listFilesGrouped() {
        // collectionName ASC, createdAt DESC 로 정렬된 전체를 뽑아온 뒤, 메모리에서 그룹핑
        Sort sort = Sort.by(Sort.Order.asc("collectionName"), Sort.Order.desc("createdAt"));
        List<BotFile> all = botFileRepository.findAll(sort);

        Map<String, List<CollectionFileItemResponse>> grouped = new LinkedHashMap<>();
        for (BotFile b : all) {
            grouped.computeIfAbsent(b.getCollectionName(), k -> new ArrayList<>())
                    .add(new CollectionFileItemResponse(
                            b.getId(),
                            b.getOriginalFilename(),
                            b.getCreatedAt()
                    ));
        }

        List<CollectionFilesGroupResponse> result = new ArrayList<>(grouped.size());
        for (Map.Entry<String, List<CollectionFileItemResponse>> e : grouped.entrySet()) {
            result.add(new CollectionFilesGroupResponse(e.getKey(), e.getValue()));
        }
        return result;
    }

    /**
     * 폴더(컬렉션) 이름 목록 조회
     */
    @Transactional(readOnly = true)
    public CollectionNamesResponse listCollectionNames() {
        List<String> names = botFileRepository.findDistinctCollectionNames(); // ASC 정렬됨
        return new CollectionNamesResponse(names);
    }
}