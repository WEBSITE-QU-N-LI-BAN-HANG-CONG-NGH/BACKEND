package com.webanhang.team_project.dto.cart;

import com.webanhang.team_project.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String size;
    private int quantity;
    private int price;
    private int discountedPrice;
    private String productName;
    private String imageUrl;
    private int discountPercent;
}
