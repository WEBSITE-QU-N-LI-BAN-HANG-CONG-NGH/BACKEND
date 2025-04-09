package com.webanhang.team_project.repository;


import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    User findByEmail(String email);

    // Tìm kiếm người dùng theo email hoặc tên
    List<User> findByEmailContainingOrFirstNameContainingOrLastNameContaining(
            String email, String firstName, String lastName, Pageable pageable);

    // Lọc người dùng theo role
    List<User> findByRoleName(UserRole roleName, Pageable pageable);

    // Kết hợp tìm kiếm và lọc
    List<User> findByEmailContainingOrFirstNameContainingOrLastNameContainingAndRoleName(
            String email, String firstName, String lastName, UserRole roleName, Pageable pageable);
}
