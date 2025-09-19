package com.community.demo.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CollectionNamesResponse {
    private List<String> collections;  // 폴더 이름 목록
}