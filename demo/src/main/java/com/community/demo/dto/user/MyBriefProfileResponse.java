package com.community.demo.dto.user;

import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyBriefProfileResponse {

    private final Long userId;
    private final String username;
    private final String student_number;
    private final String department;
    private final RoleType roleType;
    private final ProfileImage profileImage; // nested object

    // ★ 생성자는 private 그대로 둬도 됩니다.
    private MyBriefProfileResponse(Long userId, String username, String student_number,
                                   String department, RoleType roleType, ProfileImage profileImage) {
        this.userId = userId;
        this.username = username;
        this.student_number = student_number;
        this.department = department;
        this.roleType = roleType;
        this.profileImage = profileImage;
    }

    // nested class는 public static 이어야 서비스에서 타입을 사용할 수 있어요.
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileImage {
        private final String url; // 예: "/files/profiles/5/face.jpg"
    }

    // ★ 여기서 공개 URL까지 만들어서 세팅
    public static MyBriefProfileResponse from(User u) {
        String url = toPublicUrlCompat(u.getProfileImageUrl());
        ProfileImage img = (url == null ? null : new ProfileImage(url));
        return new MyBriefProfileResponse(
                u.getId(),
                u.getUsername(),
                u.getStudent_number(),
                u.getDepartment(),
                u.getRoleType(),
                img
        );
    }

    // DB에 "profiles/5/xxx.jpg"처럼 저장돼 있어도, 과거에 "/files/..."로 저장돼 있어도 호환되도록
    private static String toPublicUrlCompat(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (stored.startsWith("/files/")) return stored;
        return "/files/" + stored;
    }
}