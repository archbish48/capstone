package com.community.demo.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreditsFlatRequest {

    @JsonProperty("전공")
    private Integer major;                    // 단일전공자 최소전공이수학점의 '취득학점'에 대응

    @JsonProperty("기초전공")
    private Integer basicMajor;

    @JsonProperty("교양필수")
    private Integer generalRequired;

    @JsonProperty("부/복수 전공")
    private Integer subOrDoubleMajor;         // 트랙에 따라 부/복 최소전공이수학점으로 저장

    @JsonProperty("부/복수 기초전공")
    private Integer subOrDoubleBasicMajor;    // 트랙에 따라 부/복 기초전공으로 저장

    @JsonProperty("총 이수학점")
    private Integer total;

    @JsonProperty("평점")
    private java.math.BigDecimal gpa;         // 선택
}