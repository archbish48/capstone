package com.community.demo.dto.enroll;

import com.community.demo.domain.user.EnrollMode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FinishRequest {
    private EnrollMode mode; // BASIC or CART
}