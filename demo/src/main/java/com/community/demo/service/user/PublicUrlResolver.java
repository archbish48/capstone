package com.community.demo.service.user;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class PublicUrlResolver {

    /**
     * 저장된 경로(예: "profiles/5/a.png" 또는 "/files/profiles/5/a.png")를
     * 현재 요청의 호스트/포트 기준 절대 URL로 변환합니다.
     * 예) http://localhost:8080/files/profiles/5/a.png
     */
    public String toAbsolute(String stored) {
        if (!StringUtils.hasText(stored)) return null;

        // 이미 절대 URL이면 그대로 반환
        if (stored.startsWith("http://") || stored.startsWith("https://")) {
            return stored;
        }

        // "/files/..." 가 아니면 prefix 붙이기
        String rel = stored.startsWith("/files/") ? stored : "/files/" + stored;

        return ServletUriComponentsBuilder
                .fromCurrentContextPath()  // http(s)://host:port
                .path(rel)
                .toUriString();
    }
}