package com.webanhang.team_project.dto.cart;

import lombok.Data;

@Data
public class AddItemRequest {
    private Long productId;
    private String size;
    private int quantity;
}
