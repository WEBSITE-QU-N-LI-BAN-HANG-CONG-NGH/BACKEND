package com.ecommerce.DTO;

import com.ecommerce.model.Cart;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CartDTO {
    private Long id;
    private int totalPrice;      // Tổng giá gốc
    private int totalItems;      // Tổng số lượng sản phẩm
    private int totalDiscountedPrice;  // Tổng giá sau giảm giá
    private int total;           // Tổng tiền phải trả
    private List<CartItemDTO> cartItems;

    public CartDTO(Cart cart) {
        this.id = cart.getId();
        this.totalPrice = cart.getTotalPrice();
        this.totalItems = cart.getTotalItems();
        this.totalDiscountedPrice = cart.getTotalDiscountedPrice();
        this.total = cart.getTotal();
        
        if (cart.getCartItems() != null) {
            this.cartItems = cart.getCartItems().stream()
                    .map(CartItemDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
