package com.community.demo.domain.user;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


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

    @Column(unique = true)
    private String student_number;
    private String department;                  // 학과


    @Enumerated(EnumType.STRING)                // STUDENT / STAFF / MANAGER / ADMIN
    @Column(nullable = false)
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus roleStatus = RoleStatus.ACTIVE; // STUDENT 는 즉시 활성

    // 모드별 최근 5개 측정시간(ms) 캐시 (최신이 앞)
    @ElementCollection(fetch = FetchType.EAGER)     // EAGER 는 항상 즉시 로딩됨
    @CollectionTable(name = "user_recent_basic", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "duration_ms")
    @OrderColumn(name = "idx")
    private List<Long> recentBasicMs = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_recent_cart", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "duration_ms")
    @OrderColumn(name = "idx")
    private List<Long> recentCartMs = new ArrayList<>();



    // 생성자
    public User(String username, String password, String email, String student_number, RoleType roleType, String department) {      // roleType 만 받도록
        this.username  = username;
        this.password  = password;
        this.email     = email;
        this.student_number     = student_number;
        this.roleType  = roleType;
        this.department  = department;

        // STUDENT 는 ACTIVE, 그 외는 PENDING
        this.roleStatus = (roleType == RoleType.STUDENT) ? RoleStatus.ACTIVE : RoleStatus.PENDING;
    }

    // BASIC 모드 최근5 push & trim
    public void pushRecentBasic(long durationMs) {
        if (recentBasicMs == null) recentBasicMs = new ArrayList<>();
        recentBasicMs.add(0, durationMs);
        while (recentBasicMs.size() > 5) recentBasicMs.remove(recentBasicMs.size() - 1);
    }

    // CART 모드 최근5 push & trim
    public void pushRecentCart(long durationMs) {
        if (recentCartMs == null) recentCartMs = new ArrayList<>();
        recentCartMs.add(0, durationMs);
        while (recentCartMs.size() > 5) recentCartMs.remove(recentCartMs.size() - 1);
    }

}
