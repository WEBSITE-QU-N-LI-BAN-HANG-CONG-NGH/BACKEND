package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Cart;
import com.ecommerce.model.User;
import com.ecommerce.request.AddItemRequest;

public interface CartService {
    public Cart createCart(User user);

    public Cart findUserCart(Long userId) throws GlobalExceptionHandler;

    public Cart addCartItem(Long userId, AddItemRequest req) throws GlobalExceptionHandler;

    public Cart updateCartItem(Long userId, Long itemId, AddItemRequest req) throws GlobalExceptionHandler;

    public void removeCartItem(Long userId, Long itemId) throws GlobalExceptionHandler;

    public void clearCart(Long userId) throws GlobalExceptionHandler;
}
