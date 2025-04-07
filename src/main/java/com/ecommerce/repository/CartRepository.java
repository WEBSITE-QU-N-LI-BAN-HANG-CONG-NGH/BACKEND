package com.ecommerce.repository;

import com.ecommerce.model.Cart;
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {
    public Cart findByUser(User user);

    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    public Cart findByUserId(@Param("userId") Long userId);

    Long user(User user);
}
