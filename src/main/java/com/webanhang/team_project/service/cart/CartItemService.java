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
    private final ICartService cartService;
    private final IProductService productService;
    private final CartRepository cartRepository;

    @Override
    public void addItemToCart(int cartId, int productId, int quantity) {
        Cart cart = cartService.getCart(cartId);
        Product product = productService.getProductById(productId);
        CartItem cartItem = cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId() == productId)
                .findFirst().orElse(new CartItem());
        if (cartItem.getId() == 0) {
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(product.getPrice());
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }
        cartItem.setTotalPrice();
        cart.addItem(cartItem);
        cartItemRepository.save(cartItem);
        cartRepository.save(cart);
    }

    @Override
    public void removeItemFromCart(int cartId, int productId) {
        Cart cart = cartService.getCart(cartId);
        CartItem itemToRemove = getCartItem(cartId, productId);
        cart.removeItem(itemToRemove);
        cartRepository.save(cart);
    }

    @Override
    public void updateItemQuantity(int cartId, int productId, int quantity) {
        Cart cart = cartService.getCart(cartId);
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId() == productId)
                .findFirst().ifPresent(item -> {
                    item.setQuantity(quantity);
                    item.setUnitPrice(item.getProduct().getPrice());
                    item.setTotalPrice();
                });
        BigDecimal totalAmount = cart.getItems().stream().map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);
    }

    @Override
    public CartItem getCartItem(int cartId, int productId) {
        Cart cart = cartService.getCart(cartId);
        return cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId() == productId)
                .findFirst().orElseThrow(() -> new EntityNotFoundException("Cart not found!"));
    }

    public CartItemService(CartItemRepository cartItemRepository, UserService userService, CartRepository cartRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userService = userService;
        this.cartRepository = cartRepository;
    }

    @Override
    public CartItem addCartItem(CartItem cartItem) throws GlobalExceptionHandler {
        try {
            return cartItemRepository.save(cartItem);
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Error adding cart item: " + e.getMessage(), "CART_ITEM_ADD_ERROR");
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
