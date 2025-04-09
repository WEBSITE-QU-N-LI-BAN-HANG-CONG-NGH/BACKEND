package com.webanhang.team_project.service.order;



import com.webanhang.team_project.dto.order.OrderDto;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Order;

import java.util.List;

public interface IOrderService {
    Order placeOrder(int userId);
    List<OrderDto> getUserOrders(int userId);

    OrderDto convertToDto(Order order);

    public Order findOrderById(Long orderId) throws GlobalExceptionHandler;
    public List<Order> userOrderHistory(Long userId) throws GlobalExceptionHandler;
    public Order placeOrder(Long addressId, User user) throws GlobalExceptionHandler;
    public Order confirmedOrder(Long orderId) throws GlobalExceptionHandler;
    public Order shippedOrder(Long orderId) throws GlobalExceptionHandler;
    public Order deliveredOrder(Long orderId) throws GlobalExceptionHandler;
    public Order cancelOrder(Long orderId) throws GlobalExceptionHandler;
    public List<Order> getAllOrders() throws GlobalExceptionHandler;
    public void deleteOrder(Long orderId) throws GlobalExceptionHandler;
}
