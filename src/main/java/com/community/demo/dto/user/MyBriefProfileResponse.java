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

    // 저장값("profiles/5/xxx.jpg" 또는 "/files/profiles/5/xxx.jpg") → 절대 URL
    private static String toPublicAbsoluteUrl(String stored) {
        if (!StringUtils.hasText(stored)) return null;
        String rel = stored.startsWith("/files/") ? stored : "/files/" + stored;
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()   // http(s)://host:port
                .path(rel)
                .toUriString();
    }
}