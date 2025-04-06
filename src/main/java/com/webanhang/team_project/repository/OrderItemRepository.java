package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByProductId(int productId);
}
