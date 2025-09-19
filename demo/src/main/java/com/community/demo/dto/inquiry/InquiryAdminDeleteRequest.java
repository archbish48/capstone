package com.community.demo.dto.inquiry;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class InquiryAdminDeleteRequest {
    @NotEmpty
    private List<Long> pids; // 삭제할 문의 pid 리스트
}