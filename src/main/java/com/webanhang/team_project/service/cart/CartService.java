package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.dto.AddItemRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.CartItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.CartItemRepository;
import com.webanhang.team_project.repository.CartRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;


    @Override
    public Cart getCart(int cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found!"));
        BigDecimal totalAmount = cart.getTotalAmount();
        cart.setTotalAmount(totalAmount);
        return cartRepository.save(cart);
    }

    @Override
    public Cart getCartByUserId(int userId) {
        return cartRepository.findByUserId(userId);
    }


    @Override
    public void clearCart(int cartId) {
        Cart cart = getCart(cartId);
        cartItemRepository.deleteAllByCartId(cartId);
        cart.clearCart();
        cartRepository.deleteById(cartId);
    }


    @Override
    public Cart initializeNewCartForUser(User user) {
        return Optional.ofNullable(getCartByUserId(user.getId()))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public BigDecimal getTotalPrice(int cartId) {
        Cart cart = getCart(cartId);
        return cart.getTotalAmount();
    }

    @Override
    public Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private Cart createCart(Long userId) throws GlobalExceptionHandler {
        User user = userService.findUserById(userId);
        return createCart(user);
    }

    @Override
    public Cart findUserCart(Long userId) throws GlobalExceptionHandler {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            // Tạo giỏ hàng mới nếu chưa có
            cart = createCart(userId);
        }
        return cart;
    }

    @Override
    public Cart addCartItem(Long userId, AddItemRequest req) throws GlobalExceptionHandler{
        Cart cart = findUserCart(userId);
        Product product = productService.findProductById(req.getProductId());

        // Kiểm tra sản phẩm có tồn tại trong giỏ hàng chưa
        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(req.getProductId())
                        && item.getSize().equals(req.getSize()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Cập nhật số lượng nếu sản phẩm đã tồn tại
            existingItem.setQuantity(existingItem.getQuantity() + req.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            // Thêm sản phẩm mới vào giỏ hàng
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setSize(req.getSize());
            newItem.setQuantity(req.getQuantity());
            newItem.setPrice(product.getPrice());
            newItem.setDiscountedPrice(product.getDiscountedPrice());
            cart.getCartItems().add(newItem);
            newItem.setDiscountPercent(product.getDiscountPersent());
            cartItemRepository.save(newItem);
        }

        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart updateCartItem(Long userId, Long itemId, AddItemRequest req) throws GlobalExceptionHandler {
        Cart cart = findUserCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new GlobalExceptionHandler("Cart item not found", "ITEM_NOT_FOUND"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new GlobalExceptionHandler("Cart item does not belong to user", "INVALID_ITEM");
        }

        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);

        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    @Override
    public void removeCartItem(Long userId, Long itemId) throws GlobalExceptionHandler {
        Cart cart = findUserCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new GlobalExceptionHandler("Cart item not found", "ITEM_NOT_FOUND"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new GlobalExceptionHandler("Cart item does not belong to user", "INVALID_ITEM");
        }

        cart.getCartItems().remove(item);
        cartItemRepository.delete(item);

        updateCartTotals(cart);
        cartRepository.save(cart);
    }

    @Override
    @Transactional  // Thêm annotation này
    public void clearCart(Long userId) throws GlobalExceptionHandler {
        Cart cart = findUserCart(userId);

        // Xóa các mục từ bảng cart_items trước
        ICartItemService.deleteAllCartItems(cart.getId(), userId);

        // Cập nhật đối tượng cart
        cart.getCartItems().clear();
        cart.setTotalItems(0);
        cart.setTotalPrice(0);
        cart.setTotalDiscountedPrice(0);
        cart.setDiscount(0);

        // Lưu giỏ hàng đã được cập nhật
        cartRepository.save(cart);
    }
    private void updateCartTotals(Cart cart) {
        List<CartItem> items = cart.getCartItems();

        int totalPrice = items.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        int totalDiscountedPrice = items.stream()
                .mapToInt(item -> item.getDiscountedPrice() * item.getQuantity())
                .sum();

        int totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        cart.setTotalPrice(totalPrice);
        cart.setTotalDiscountedPrice(totalDiscountedPrice);
        cart.setTotalItems(totalItems);
        cart.setDiscount(totalPrice - totalDiscountedPrice);
    }
}

