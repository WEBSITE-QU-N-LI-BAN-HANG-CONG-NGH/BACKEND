package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.dto.AddItemRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.User;

import java.math.BigDecimal;

public interface ICartService {
    Cart getCart(Long cartId);

    Cart getCartByUserId(Long userId);

    void clearCart(Long cartId);

    Cart initializeNewCartForUser(User user);

    BigDecimal getTotalPrice(int cartId);

    public Cart createCart(User user);

    public Cart findUserCart(Long userId);

    public Cart addCartItem(Long userId, AddItemRequest req);

    public Cart updateCartItem(Long userId, Long itemId, AddItemRequest req);

    public void removeCartItem(Long userId, Long itemId);

    public void clearCart(Long userId);
}

