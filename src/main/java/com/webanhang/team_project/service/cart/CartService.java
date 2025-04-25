package com.webanhang.team_project.service.cart;


import com.webanhang.team_project.dto.cart.AddItemRequest;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.CartItemRepository;
import com.webanhang.team_project.repository.CartRepository;
import com.webanhang.team_project.service.product.IProductService;
import com.webanhang.team_project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final IProductService productService;
    private final UserService userService;
    private final ICartItemService cartItemService;


    @Override
    public Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private Cart createCart(Long userId)  {
        User user = userService.getUserById(userId);
        return createCart(user);
    }

    @Override
    public Cart findUserCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            // Tạo giỏ hàng mới nếu chưa có
            cart = createCart(userId);
        }
        return cart;
    }
    
    @Override
    public Cart getCartByUserId(Long userId) {
        return findUserCart(userId);
    }
    
    @Override
    public Cart initializeNewCartForUser(User user) {
        Cart existingCart = cartRepository.findByUserId(user.getId());
        if (existingCart != null) {
            return existingCart;
        }
        return createCart(user);
    }

    @Override
    public Cart addCartItem(Long userId, AddItemRequest req) {
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

            ProductSize productSize = product.getSizes().stream()
                    .filter(item -> item.getName().equals(req.getSize()))
                    .findFirst()
                    .orElse(null);

            if (existingItem.getQuantity() > productSize.getQuantity()) {
                throw new RuntimeException("Số lượng sản phẩm không đủ trong kho");
            }

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
    public Cart updateCartItem(Long userId, Long itemId, AddItemRequest req) {
        Cart cart = findUserCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        Product product = productService.findProductById(req.getProductId());

        ProductSize productSize = product.getSizes().stream()
                .filter(ps -> ps.getName().equals(req.getSize()))
                .findFirst()
                .orElse(null);

        if (req.getQuantity() > productSize.getQuantity()) {
            throw new RuntimeException("Số lượng sản phẩm không đủ trong kho");
        }

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);

        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    @Override
    public void removeCartItem(Long userId, Long itemId) {
        Cart cart = findUserCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        cart.getCartItems().remove(item);
        cartItemRepository.delete(item);

        updateCartTotals(cart);
        cartRepository.save(cart);
    }

    @Override
    @Transactional  // Thêm annotation này
    public void clearCart(Long userId) {
        Cart cart = findUserCart(userId);

        // Xóa các mục từ bảng cart_items trước
        cartItemService.deleteAllCartItems(cart.getId(), userId);

        // Cập nhật đối tượng cart
        cart.getCartItems().clear();
        cart.setTotalItems(0);
        cart.setOriginalPrice(0);
        cart.setTotalDiscountedPrice(0);

        // Lưu giỏ hàng đã được cập nhật
        cartRepository.save(cart);
    }

    private void updateCartTotals(Cart cart) {
        Set<CartItem> items = cart.getCartItems(); // use set

        int totalOriginalPrice = items.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        int totalDiscountedPrice = items.stream()
                .mapToInt(item -> item.getDiscountedPrice() * item.getQuantity())
                .sum();

        int totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        cart.setOriginalPrice(totalOriginalPrice);
        cart.setTotalDiscountedPrice(totalDiscountedPrice);
        cart.setTotalItems(totalItems);
        cart.setDiscount(totalOriginalPrice - totalDiscountedPrice);
    }
}

