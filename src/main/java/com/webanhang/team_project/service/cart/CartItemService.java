package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.CartItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.CartItemRepository;
import com.webanhang.team_project.repository.CartRepository;
import com.webanhang.team_project.service.product.IProductService;
import com.webanhang.team_project.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService {
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ICartService cartService;
    private final IProductService productService;


    public CartItemService(CartItemRepository cartItemRepository, UserService userService, CartRepository cartRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userService = userService;
        this.cartRepository = cartRepository;
    }

    @Override
    public CartItem addCartItem(CartItem cartItem) {
        try {
            return cartItemRepository.save(cartItem);
        } catch (Exception e) {
            throw new RuntimeException("Error adding cart item: " + e.getMessage(), "CART_ITEM_ADD_ERROR");
        }
    }

    @Override
    public CartItem updateCartItem(Long cartItemId, CartItem cartItem) throws GlobalExceptionHandler {
        CartItem existingItem = getCartItemById(cartItemId);
        try {
            cartItem.setId(cartItemId);
            return cartItemRepository.save(cartItem);
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Error updating cart item: " + e.getMessage(), "CART_ITEM_UPDATE_ERROR");
        }
    }

    @Override
    public void deleteCartItem(Long cartItemId) throws GlobalExceptionHandler {
        try {
            cartItemRepository.deleteById(cartItemId);
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Error deleting cart item: " + e.getMessage(), "CART_ITEM_DELETE_ERROR");
        }
    }

    @Override
    public CartItem getCartItemById(Long cartItemId) throws GlobalExceptionHandler {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new GlobalExceptionHandler("Cart item not found with id: " + cartItemId, "CART_ITEM_NOT_FOUND"));
    }

    @Override
    public boolean isCartItemExist(Long cartId, Long productId, String size) throws GlobalExceptionHandler {
        return cartItemRepository.existsByCartIdAndProductIdAndSize(cartId, productId, size);
    }

    @Override
    public CartItem createCartItem(CartItem cartItem) {
        return cartItemRepository.save(cartItem);
    }

    @Override
    public CartItem updateCartItem(Long userId, Long id, CartItem cartItem) throws GlobalExceptionHandler {
        try {
            CartItem existingItem = getCartItemById(id);
            cartItem.setId(id);
            return cartItemRepository.save(cartItem);
        } catch (GlobalExceptionHandler e) {
            throw new GlobalExceptionHandler(e.getMessage(), e.getCode());
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Error updating cart item: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllCartItems(Long cartId, Long userId) throws GlobalExceptionHandler {
        try {
            cartItemRepository.deleteByCartId(cartId);
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Error deleting all cart items: " + e.getMessage());
        }
    }

    @Override
    public CartItem isCartItemExist(Cart cart, Product product, String size, Long userId) throws GlobalExceptionHandler {
        try {
            return cartItemRepository.isCartItemExist(cart, product, size, userId);
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Error checking cart item existence: " + e.getMessage());
        }
    }

    @Override
    public CartItem findCartItemById(Long cartItemId) throws GlobalExceptionHandler {
        try {
            return getCartItemById(cartItemId);
        } catch (GlobalExceptionHandler e) {
            throw new GlobalExceptionHandler(e.getMessage(), e.getCode());
        }
    }
}
