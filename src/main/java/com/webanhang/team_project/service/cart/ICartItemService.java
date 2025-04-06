package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.model.CartItem;

public interface ICartItemService {
    void addItemToCart(int cartId, int productId, int quantity);
    void removeItemFromCart(int cartId, int productId);
    void updateItemQuantity(int cartId, int productId, int quantity);
    CartItem getCartItem(int cartId, int productId);
}
