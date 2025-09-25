package com.community.demo.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class PublicUrlResolver {

    private final String publicBaseUrl;

    public PublicUrlResolver(@Value("${app.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String toAbsolute(String stored) {
        if (!StringUtils.hasText(stored)) return null;

        if (stored.startsWith("http://") || stored.startsWith("https://")) {
            return stored;
        }

        String rel = stored.startsWith("/files/") ? stored : "/files/" + stored;

        // 설정 값으로 주입된 외부 URL을 사용합니다.
        return publicBaseUrl + rel;
    }
}