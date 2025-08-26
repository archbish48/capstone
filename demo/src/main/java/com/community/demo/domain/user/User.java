package com.community.demo.domain.user;


import jakarta.persistence.*;
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

    @Column(unique = true)
    private String student_number;
    private String department;                  // 학과


    @Enumerated(EnumType.STRING)                // STUDENT / STAFF / MANAGER / ADMIN
    @Column(nullable = false)
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus roleStatus = RoleStatus.ACTIVE; // STUDENT 는 즉시 활성

    // 수강신청 최고 기록 (단위: 밀리초). null 이면 아직 기록 없음으로 간주
    @Column(name = "best_enroll_record_ms")
    private Long bestEnrollRecordMs;



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

    // 주어진 durationMs가 더 짧으면 신기록 갱신
    public boolean updateBestIfBetter(long durationMs) {
        if (bestEnrollRecordMs == null || durationMs < bestEnrollRecordMs) {
            bestEnrollRecordMs = durationMs;
            return true;
        }
        return false;
    }

}
