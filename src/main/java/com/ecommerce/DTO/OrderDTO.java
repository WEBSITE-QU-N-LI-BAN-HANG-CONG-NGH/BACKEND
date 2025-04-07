package com.ecommerce.DTO;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.model.Order;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private OrderStatus orderStatus; // Thêm orderStatus
    private int totalAmount;
    private Integer totalDiscountedPrice; // Sửa thành Integer để đồng bộ với Order
    private int discount; // Thêm discount
    private int totalItems;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private AddressDTO shippingAddress;
    private PaymentStatus paymentStatus;
    private List<OrderItemDTO> orderItems;

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.orderStatus = order.getOrderStatus();
        this.totalAmount = order.getTotalAmount();
        this.totalDiscountedPrice = order.getTotalDiscountedPrice() != null ? order.getTotalDiscountedPrice() : 0;
        this.discount = order.getDiscount();
        this.totalItems = order.getTotalItems();
        this.orderDate = order.getOrderDate();
        this.deliveryDate = order.getDeliveryDate();
        this.shippingAddress = new AddressDTO(order.getShippingAddress());
        this.paymentStatus = order.getPaymentStatus();
        this.orderItems = new ArrayList<>();
        order.getOrderItems().forEach(item -> this.orderItems.add(new OrderItemDTO(item)));
    }
}