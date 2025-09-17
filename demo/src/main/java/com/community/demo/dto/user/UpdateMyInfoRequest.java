package com.community.demo.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateMyInfoRequest {
    private String username;         // 이름
    private String department;       // 학과
    private String studentNumber;    // 학번
    private String gradeLabel;       // "1학년 2학기" (선택: 미전송 시 앵커 유지)

    // 부전공/복수전공은 둘 중 하나만 보내기 (동시 전송 시 400)
    private String minor;            // 부전공 (보내면 복수전공은 null 처리)
    private String doubleMajor;      // 복수전공 (보내면 부전공은 null 처리)
}
