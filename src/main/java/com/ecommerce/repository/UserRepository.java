package com.ecommerce.repository;

//Mục đích: Interface repository để tương tác với bảng users.
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    public User findByEmail(String email);
}
