package com.community.demo.dto.user;

import com.community.demo.domain.user.RoleStatus;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import lombok.Data;

@Data
public class MyProfileResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final String student_number;
    private final String department;
    private final RoleType roleType;
    private final RoleStatus roleStatus;
    private final String profileImageUrl;

    private MyProfileResponse(Long id, String username, String email, String student_number,
                              String department, RoleType roleType, RoleStatus roleStatus,
                              String profileImageUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.student_number = student_number;
        this.department = department;
        this.roleType = roleType;
        this.roleStatus = roleStatus;
        this.profileImageUrl = profileImageUrl;
    }

    public static MyProfileResponse from(User u) {
        return new MyProfileResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getStudent_number(),
                u.getDepartment(),
                u.getRoleType(),
                u.getRoleStatus(),
                u.getProfileImageUrl()
        );
    }
}