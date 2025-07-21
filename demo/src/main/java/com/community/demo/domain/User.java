package com.community.demo.domain;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {     //유저 정보 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;

    @Column(nullable = false, unique = true)
    private String email;
    private String phone;
    private String department;                  // 학과


    @Enumerated(EnumType.STRING)                // STUDENT / STAFF / MANAGER / ADMIN
    @Column(nullable = false)
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus roleStatus = RoleStatus.ACTIVE; // STUDENT 는 즉시 활성


    private String profileImageUrl; // 프로필 이미지 변수(null 이면 default 이미지 또는 프사 없음으로 간주)



    // 생성자
    public User(String username, String password, String email, String phone, RoleType roleType, String department) {      // roleType 만 받도록
        this.username  = username;
        this.password  = password;
        this.email     = email;
        this.phone     = phone;
        this.roleType  = roleType;
        this.department  = department;

        // STUDENT 는 ACTIVE, 그 외는 PENDING
        this.roleStatus = (roleType == RoleType.STUDENT) ? RoleStatus.ACTIVE : RoleStatus.PENDING;
    }



}
