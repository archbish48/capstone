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
    private final ProfileImage profileImage; // ← 추가

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileImage {
        private final String url; // 예: "/files/profiles/5/face.jpg"
        public ProfileImage(String url) { this.url = url; }
    }

    private MyBriefProfileResponse(Long userId, String username, String student_number,
                                   String department, RoleType roleType, ProfileImage profileImage) {
        this.userId = userId;
        this.username = username;
        this.student_number = student_number;
        this.department = department;
        this.roleType = roleType;
        this.profileImage = profileImage;
    }

    public static MyBriefProfileResponse from(User u) {
        String storagePath = u.getProfileImageUrl(); // ex) "profiles/5/face.jpg" (상대경로)
        String url = (storagePath == null) ? null : "/files/" + storagePath; // 커뮤니티와 동일한 형태
        return new MyBriefProfileResponse(
                u.getId(),
                u.getUsername(),
                u.getStudent_number(),
                u.getDepartment(),
                u.getRoleType(),
                (url != null ? new ProfileImage(url) : null)
        );
    }
}