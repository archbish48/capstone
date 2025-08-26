package com.community.demo.repository;

import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByDepartmentAndRoleType(@NotBlank String department, RoleType roleType);


    // 모든 유저의 최고 기록 평균(ms) 반환, 기록 없는 유저(null)는 제외해서 계산
    @Query("select avg(u.bestEnrollRecordMs) from User u where u.bestEnrollRecordMs is not null")
    Double averageBestRecordMs();

}
