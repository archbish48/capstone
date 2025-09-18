package com.community.demo.dto.user;

import com.community.demo.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreditInfoResponse {
    private final int majorCredits;            // 전공
    private final int basicMajorCredits;       // 기초전공
    private final int generalRequiredCredits;  // 교양필수
    private final int totalCredits;            // 총이수학점
    private final BigDecimal gpa;              // 학점평점 (예: 3.85)

    public static CreditInfoResponse from(User u) {
        int major   = safe(u.getCreditsMajor());
        int basic   = safe(u.getCreditsBasicMajor());
        int general = safe(u.getCreditsGeneralRequired());

        int total = (u.getCreditsTotal() != null && u.getCreditsTotal() > 0)
                ? u.getCreditsTotal()
                : (major + basic + general);

        return CreditInfoResponse.builder()
                .majorCredits(major)
                .basicMajorCredits(basic)
                .generalRequiredCredits(general)
                .totalCredits(total)
                .gpa(u.getGpa())  // null이면 응답에 null 내려감
                .build();
    }

    private static int safe(Integer v) {
        return v == null ? 0 : v;
    }
}