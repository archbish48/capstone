package com.community.demo.dto;

import com.community.demo.domain.ReactionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionRequest {
    private ReactionType type;   // LIKE / DISLIKE
}
