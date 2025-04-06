package com.webanhang.team_project.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private int productId;
    private String productName;
    private String productBrand;
    private int quantity;
    private BigDecimal price;
}
