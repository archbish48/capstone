package com.community.demo.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ChatRequest {
    @NotBlank
    private String question;
    //private String collectionName; // 현재 FastAPI 는 사용 안 함

}