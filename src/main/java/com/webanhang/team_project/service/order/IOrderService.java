package com.webanhang.team_project.service.order;



import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.User;

import java.util.List;

public interface IOrderService {
//    Order placeOrder(int userId);
//    List<OrderDTO> getUserOrders(int userId);
//
//    OrderDTO convertToDto(Order order);

    public Order findOrderById(Long orderId);
    public List<Order> userOrderHistory(Long userId);
    public Order placeOrder(Long addressId, User user);
    public Order confirmedOrder(Long orderId);
    public Order shippedOrder(Long orderId);
    public Order deliveredOrder(Long orderId);
    public Order cancelOrder(Long orderId);
    public List<Order> getAllOrders();
    public void deleteOrder(Long orderId);
}
