package com.webanhang.team_project.service.order;



import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.dto.order.OrderDetailDTO;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IOrderService {
    OrderDTO convertToDto(Order order);

    public Order findOrderById(Long orderId);
    public List<Order> userOrderHistory(Long userId);
    List <Order> placeOrder(Long addressId, User user);
    public Order confirmedOrder(Long orderId);
    public Order shippedOrder(Long orderId);
    public Order deliveredOrder(Long orderId);
    public Order cancelOrder(Long orderId);
    public List<Order> getAllOrders();
    public void deleteOrder(Long orderId);
    public Map<String, Object> getOrderStatistics(LocalDate start, LocalDate end);
    List<OrderDetailDTO> getAllOrdersByJF();

    List<Order> getPendingOrders();

    List<Order> getConfirmedOrders();

    List<Order> getShippedOrders();

    List<Order> getDeliveredOrders();

    List<Order> getCancelledOrders();
}
