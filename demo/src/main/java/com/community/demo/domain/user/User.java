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
// (선택) Hibernate 사용 시, 부전공과 복수전공이 동시에 채워지지 않도록 체크
// @org.hibernate.annotations.Check(constraints = "(minor_department IS NULL) <> (double_major_department IS NULL)")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String student_number;

    private String department;                  // 주전공(학과)

    @Enumerated(EnumType.STRING)                // STUDENT / STAFF / MANAGER / ADMIN
    @Column(nullable = false)
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus roleStatus = RoleStatus.ACTIVE;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    // ====== [추가] 학사 정보 ======
    // === 부전공/복수전공(둘 중 하나만) ===
    private String minorDepartment;        // 부전공
    private String doubleMajorDepartment;  // 복수전공

    // ----- [추가] 자동 증가의 '앵커(기준점)' -----
    // 사용자가 수정할 때마다 여기 값과 날짜를 갱신. 조회 시 이 기준으로 자동 증가 계산.
    @Column(name = "progress_anchor_grade_year")
    private Integer progressAnchorGradeYear;    // 앵커 학년 (1~4)

    @Column(name = "progress_anchor_semester")
    private Integer progressAnchorSemester;     // 앵커 학기 (1 또는 2)

    @Column(name = "progress_anchor_date")
    private java.time.LocalDate progressAnchorDate; // 앵커 기준일(Asia/Seoul의 날짜)

    // === (중요) 자동 전환을 위한 '입학 기준' 저장 ===
    @Column(name = "entry_year")
    private Integer entryYear;             // 입학년도 (예: 2023)

    @Column(name = "entry_semester")
    private Integer entrySemester;         // 입학학기 (1 또는 2)

    @Column(name = "credits_major")
    private Integer creditsMajor = 0;           // 전공 이수학점

    @Column(name = "credits_basic_major")
    private Integer creditsBasicMajor = 0;      // 기초전공 이수학점

    @Column(name = "credits_general_required")
    private Integer creditsGeneralRequired = 0; // 교양필수 이수학점

    @Column(name = "credits_total")
    private Integer creditsTotal = 0;           // 총 이수학점 (없으면 서비스에서 합산해도 됨)

    @Column(name = "gpa", precision = 3, scale = 2)
    private java.math.BigDecimal gpa;           // 학점평점 (예: 3.85)
    // =================================

    // 기존 필드 (예: 최근 측정 캐시) 생략 없이 유지
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_recent_basic", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "duration_ms")
    @OrderColumn(name = "idx")
    private java.util.List<Long> recentBasicMs = new java.util.ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_recent_cart", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "duration_ms")
    @OrderColumn(name = "idx")
    private java.util.List<Long> recentCartMs = new java.util.ArrayList<>();

    public User(String username, String password, String email, String student_number, RoleType roleType, String department) {
        this.username  = username;
        this.password  = password;
        this.email     = email;
        this.student_number = student_number;
        this.roleType  = roleType;
        this.department = department;
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
