package com.community.demo.dto.user;

import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyBriefProfileResponse {

    private final Long userId;
    private final String username;
    private final String student_number;
    private final String department;
    private final RoleType roleType;
    private final ProfileImage profileImage;

    private MyBriefProfileResponse(Long userId, String username, String student_number,
                                   String department, RoleType roleType, ProfileImage profileImage) {
        this.userId = userId;
        this.username = username;
        this.student_number = student_number;
        this.department = department;
        this.roleType = roleType;
        this.profileImage = profileImage;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileImage {
        private final String url; // 절대 URL
    }

    public static MyBriefProfileResponse from(User u) {
        String abs = toPublicAbsoluteUrl(u.getProfileImageUrl()); // ← 여기서 절대 URL 변환
        ProfileImage img = (abs == null ? null : new ProfileImage(abs));
        return new MyBriefProfileResponse(
                u.getId(),
                u.getUsername(),
                u.getStudent_number(),
                u.getDepartment(),
                u.getRoleType(),
                img
        );
    }

    private static String toPublicAbsoluteUrl(String stored) {
        if (!StringUtils.hasText(stored)) return null;

        // stored 값이 어떤 형태든 항상 "/files/..."로 시작하도록 경로를 보정합니다.
        String rel = stored;
        if (!stored.startsWith("/files/")) {
            if (stored.startsWith("files/")) {
                // "files/..."로 시작하면 앞에 "/"만 붙여줍니다.
                rel = "/" + stored;
            } else {
                // 둘 다 아니면 "/files/"를 통째로 붙여줍니다.
                rel = "/files/" + stored;
            }
        }

        // ✨ 전체 URL 빌더를 사용하지 않고, 보정된 상대 경로(rel)를 바로 반환합니다.
        return rel;
    }
}