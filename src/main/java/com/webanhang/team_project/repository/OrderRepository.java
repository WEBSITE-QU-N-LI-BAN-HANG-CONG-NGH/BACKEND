package com.webanhang.team_project.repository;


import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByOrderDateBetweenAndOrderStatus(LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
    List<Order> findByOrderDateGreaterThanEqualAndOrderStatus(LocalDateTime startDate, OrderStatus status);
    List<Order> findByOrderStatus(OrderStatus status);
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
    @Modifying
    @Query("DELETE FROM Order o WHERE o.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
