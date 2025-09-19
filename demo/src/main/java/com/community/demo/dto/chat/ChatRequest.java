package com.community.demo.dto.chat;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Getter @Setter @NoArgsConstructor
public class ChatRequest {
    @NotBlank(message = "question must not be blank")
    @JsonProperty("question")
    @JsonAlias({"content", "text", "q", "message"})
    private String question;
}