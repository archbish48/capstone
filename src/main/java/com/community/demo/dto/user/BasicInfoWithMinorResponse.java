package com.community.demo.dto.user;

import com.community.demo.domain.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class BasicInfoWithMinorResponse {
    private final String username;
    private final String department;
    private final String studentNumber;

    private final Integer gradeYear;   // 계산값
    private final Integer semester;    // 계산값
    private final String gradeLabel;   // "1학년 2학기"

    private final String minor;        // 부전공

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String profileImageUrl; // 공개 URL

}