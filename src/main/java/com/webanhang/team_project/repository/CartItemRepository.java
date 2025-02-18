package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByProductId(int productId);

    void deleteAllByCartId(int cartId);
}
