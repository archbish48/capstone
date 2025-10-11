package com.community.demo.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;



@Schema(description = "질문-답변 상세")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPairDetail {

    private Long id;

    @Schema(example = "수강신청 일정 알려주세요")
    private String question;

    @Schema(example = "2025학년도 1학기 수강신청은 ...")
    private String answer;

    private LocalDateTime createdAt;

    private Long latencyMs;
}
