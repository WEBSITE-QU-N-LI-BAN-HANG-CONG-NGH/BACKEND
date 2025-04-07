package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;

public interface CartItemService {
    public CartItem createCartItem(CartItem cartItem);
    public CartItem updateCartItem(Long userId, Long id, CartItem cartItem) throws GlobalExceptionHandler;
    public void deleteAllCartItems(Long cartId, Long userId) throws GlobalExceptionHandler;
    public CartItem isCartItemExist(Cart cart, Product product, String size, Long userId) throws GlobalExceptionHandler;
    public CartItem findCartItemById(Long cartItemId) throws GlobalExceptionHandler;
    CartItem addCartItem(CartItem cartItem) throws GlobalExceptionHandler;
    CartItem updateCartItem(Long cartItemId, CartItem cartItem) throws GlobalExceptionHandler;
    void deleteCartItem(Long cartItemId) throws GlobalExceptionHandler;
    CartItem getCartItemById(Long cartItemId) throws GlobalExceptionHandler;
    boolean isCartItemExist(Long cartId, Long productId, String size) throws GlobalExceptionHandler;
}
