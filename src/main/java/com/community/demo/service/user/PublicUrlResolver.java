package com.community.demo.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class PublicUrlResolver {

    private final String publicBaseUrl;

    // 1. PUBLIC_BASE_URL (app.public-base-url) 값 주입
    // 2. server.servlet.context-path 값 주입 (없으면 빈 문자열 사용)
    public PublicUrlResolver(
            @Value("${app.public-base-url}") String rawBaseUrl,
            @Value("${server.servlet.context-path:}") String contextPath //  context-path 주입
    ) {
        String finalUrl = rawBaseUrl;

        // contextPath가 존재하고, finalUrl에 이미 contextPath가 포함되어 있지 않다면 결합
        if (StringUtils.hasText(contextPath)) {
            // finalUrl 끝 슬래시 정리
            if (finalUrl.endsWith("/")) {
                finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
            }
            // contextPath를 /route 형식으로 정리 (시작 슬래시 확인)
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }

            // finalUrl에 contextPath가 이미 포함되어 있지 않은 경우에만 추가
            if (!finalUrl.endsWith(contextPath)) {
                finalUrl = finalUrl + contextPath;
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
        // 1. stored 경로가 이미 "/files/" 또는 "files/"로 시작하는지 확인합니다.
        if (stored.startsWith("/files/")) {
            rel = stored; // 이미 완벽한 상대 경로이므로 그대로 사용합니다.
        } else if (stored.startsWith("files/")) {
            rel = "/" + stored; // "files/"로 시작하면 앞에 슬래시만 추가합니다.
        } else {
            // 2. "/files/" 접두사가 없으면 추가합니다. (이 경우가 프로필 이미지 케이스입니다)
            rel = "/files/" + stored;
        }

        // publicBaseUrl이 이제 /route를 포함하고 있으므로 바로 결합
        return publicBaseUrl + rel;
    }
}