package com.community.demo.dto.community;

import com.community.demo.domain.community.ReactionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionRequest {
    private ReactionType type;   // LIKE / DISLIKE
}
