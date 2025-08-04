package com.community.demo.repository;

import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByDepartmentAndRoleType(@NotBlank String department, RoleType roleType);
}
