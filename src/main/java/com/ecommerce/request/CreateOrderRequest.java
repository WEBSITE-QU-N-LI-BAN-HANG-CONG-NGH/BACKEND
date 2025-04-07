package com.ecommerce.request;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long addressId;
    private String paymentMethod;
    private Long cartId;
} 