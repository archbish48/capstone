package com.community.demo.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class PublicUrlResolver {

    private final String publicBaseUrl;
    private final String contextPath;

    public PublicUrlResolver(
            @Value("${app.public-base-url}") String rawBaseUrl,
            @Value("${server.servlet.context-path:}") String contextPath
    ) {
        this.contextPath = contextPath;
        // 생성자 로직은 그대로 유지!
        String finalUrl = rawBaseUrl;
        if (StringUtils.hasText(contextPath)) {
            if (finalUrl.endsWith("/")) {
                finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
            }
            String formattedContextPath = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
            if (!finalUrl.endsWith(formattedContextPath)) {
                finalUrl = finalUrl + formattedContextPath;
            }
        }
        this.publicBaseUrl = finalUrl;
    }

    public String toAbsolute(String stored) {
        if (!StringUtils.hasText(stored)) return null;

        if (stored.startsWith("http://") || stored.startsWith("https://")) {
            return stored;
        }

        String rel;
        if (stored.startsWith("/files/")) {
            rel = stored;
        } else if (stored.startsWith("files/")) {
            rel = "/" + stored;
        } else {
            rel = "/files/" + stored;
        }

        return rel;
    }
}