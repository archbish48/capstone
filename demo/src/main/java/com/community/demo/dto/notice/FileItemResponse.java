package com.community.demo.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileItemResponse {
    private Long id;     // NoticeImage.id 또는 Attachment.id
    private String url;  // "/files/..." 또는 "/uploads/..."
}
