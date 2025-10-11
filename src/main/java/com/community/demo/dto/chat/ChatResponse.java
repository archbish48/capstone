package com.community.demo.dto.chat;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private Long latencyMs;


}