package com.community.demo.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "질문 목록 아이템")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQuestionItem {

    private Long id;

    @Schema(example = "수강신청 일정 알려주세요")
    private String question;

    private LocalDateTime createdAt;
}