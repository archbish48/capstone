package com.community.demo.dto.chat;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FastApiRequest {
    private String question;
    private String collectionName;
}