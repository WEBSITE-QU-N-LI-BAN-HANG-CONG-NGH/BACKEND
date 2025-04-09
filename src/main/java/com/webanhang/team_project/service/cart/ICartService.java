package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.dto.AddItemRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.User;

import java.math.BigDecimal;

public interface ICartService {
    Cart getCart(int cartId);

    Cart getCartByUserId(int userId);

    void clearCart(int cartId);

    Cart initializeNewCartForUser(User user);

    BigDecimal getTotalPrice(int cartId);

    public Cart createCart(User user);

    public Cart findUserCart(Long userId) throws GlobalExceptionHandler;

    public Cart addCartItem(Long userId, AddItemRequest req) throws GlobalExceptionHandler;

    public Cart updateCartItem(Long userId, Long itemId, AddItemRequest req) throws GlobalExceptionHandler;

    public void removeCartItem(Long userId, Long itemId) throws GlobalExceptionHandler;

    public void clearCart(Long userId) throws GlobalExceptionHandler;
}

