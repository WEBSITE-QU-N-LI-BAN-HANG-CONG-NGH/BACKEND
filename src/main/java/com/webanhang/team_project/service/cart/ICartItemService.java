package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.CartItem;
import com.webanhang.team_project.model.Product;

public interface ICartItemService {
    void addItemToCart(int cartId, int productId, int quantity);
    void removeItemFromCart(int cartId, int productId);
    void updateItemQuantity(int cartId, int productId, int quantity);
    CartItem getCartItem(int cartId, int productId);

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
