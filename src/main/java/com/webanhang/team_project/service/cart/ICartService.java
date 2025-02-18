package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.User;

import java.math.BigDecimal;

public interface ICartService {
    Cart getCart(int cartId);

    Cart getCartByUserId(int userId);

    void clearCart(int cartId);

    Cart initializeNewCartForUser(User user);

    BigDecimal getTotalPrice(int cartId);
}

