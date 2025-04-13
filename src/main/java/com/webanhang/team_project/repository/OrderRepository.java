package com.webanhang.team_project.repository;


import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByOrderDateBetweenAndOrderStatus(LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
    List<Order> findByOrderDateGreaterThanEqualAndOrderStatus(LocalDateTime startDate, OrderStatus status);
}
