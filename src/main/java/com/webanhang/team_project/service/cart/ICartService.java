package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.dto.AddItemRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.User;

import java.math.BigDecimal;

public interface ICartService {

    public Cart createCart(User user);

    public Cart findUserCart(Long userId);

    public Cart addCartItem(Long userId, AddItemRequest req);

    public Cart updateCartItem(Long userId, Long itemId, AddItemRequest req);

    public void removeCartItem(Long userId, Long itemId);
}

