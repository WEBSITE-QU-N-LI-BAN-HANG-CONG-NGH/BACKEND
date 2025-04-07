package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Address;
import com.ecommerce.model.Order;
import com.ecommerce.model.User;

import java.util.List;

public interface OrderService {
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
