package com.community.demo.dto.user;

import com.community.demo.domain.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditInfoResponse {
    // ==== 취득학점 ====
    private final int majorCredits;                 // 전공 취득
    private final int basicMajorCredits;            // 기초전공 취득
    private final int generalRequiredCredits;       // 교양필수 취득
    private final Integer minorCredits;             // 부전공 총 취득 (있을 때만)
    private final Integer doubleMajorCredits;       // 복수전공 총 취득 (있을 때만)
    private final Integer minorBasicMajorCredits;   // 부전공 기초전공 취득 (있을 때만)
    private final Integer minorMinimumRequiredCredits;   // 부전공 최소전공이수 취득 (있을 때만)
    private final Integer doubleBasicMajorCredits;       // 복수전공 기초전공 취득 (있을 때만)
    private final Integer doubleMinimumRequiredCredits;  // 복수전공 최소전공이수 취득 (있을 때만)
    private final int totalCredits;                 // 총 이수학점(취득)
    private final BigDecimal gpa;                   // 학점평점

    // ==== 이수기준 ====
    private final Integer requiredGeneralRequiredCredits;     // req_general_required
    private final Integer requiredBasicMajorCredits;          // req_basic_major
    private final Integer requiredSingleMajorMinimumCredits;  // req_single_major_min_required
    private final Integer requiredMinorBasicMajorCredits;     // req_minor_basic_major
    private final Integer requiredMinorMinimumRequiredCredits;// req_minor_min_required
    private final Integer requiredDoubleBasicMajorCredits;    // req_double_basic_major
    private final Integer requiredDoubleMinimumRequiredCredits;// req_double_min_required
    private final Integer requiredGraduationTotal;            // req_graduation_total
    private final Integer transferRecognized;                 // 편입인정학점(Top-level)
}