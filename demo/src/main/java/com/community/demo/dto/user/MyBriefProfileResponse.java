package com.community.demo.dto.user;

import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import lombok.Data;
import lombok.Getter;

@Data
public class MyBriefProfileResponse {
    private final String username;         // 이름
    private final String student_number;   // 학번
    private final String department;       // 학과
    private final RoleType roleType;
    private final String profileImageUrl;  // 프로필 이미지

    private MyBriefProfileResponse(String username, String student_number, String department, RoleType roleType, String profileImageUrl) {
        this.username = username;
        this.student_number = student_number;
        this.department = department;
        this.roleType = roleType;
        this.profileImageUrl = profileImageUrl;
    }

    public static MyBriefProfileResponse from(User u) {
        return new MyBriefProfileResponse(
                u.getUsername(),
                u.getStudent_number(),
                u.getDepartment(),
                u.getRoleType(),
                u.getProfileImageUrl()
        );
    }
}