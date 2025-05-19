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

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.shippingAddress")
    List<Order> findAllWithUser();

    List<Order> findOrderByOrderStatus(OrderStatus status);

    List<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus orderStatus);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.shippingAddress ORDER BY o.orderDate DESC")
    List<Order> findAllWithUserOrderByOrderDateDesc();

    @Query("SELECT SUM(o.totalDiscountedPrice) FROM Order o WHERE o.sellerId = :sellerId AND o.orderStatus = :status")
    Integer sumTotalDiscountedPriceBySellerIdAndOrderStatus(
            @Param("sellerId") Long sellerId, @Param("status") OrderStatus status);

    Long countBySellerId(Long sellerId);
    List<Order> findBySellerId(Long sellerId);
    List<Order> findBySellerIdAndOrderStatus(Long sellerId, OrderStatus status);
    List<Order> findBySellerIdAndOrderDateBetween(Long sellerId, LocalDateTime startDate, LocalDateTime endDate);
    List<Order> findBySellerIdAndOrderDateBetweenAndOrderStatus(
            Long sellerId, LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
}
