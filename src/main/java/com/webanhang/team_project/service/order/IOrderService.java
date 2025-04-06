package com.webanhang.team_project.service.order;



import com.webanhang.team_project.dto.order.OrderDto;
import com.webanhang.team_project.model.Order;

import java.util.List;

public interface IOrderService {
    Order placeOrder(int userId);
    List<OrderDto> getUserOrders(int userId);

    OrderDto convertToDto(Order order);
}
