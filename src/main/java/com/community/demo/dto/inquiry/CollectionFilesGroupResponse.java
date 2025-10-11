package com.community.demo.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CollectionFilesGroupResponse {
    private String collectionName;                       // 폴더 이름
    private List<CollectionFileItemResponse> files;      // 해당 폴더의 파일들
}