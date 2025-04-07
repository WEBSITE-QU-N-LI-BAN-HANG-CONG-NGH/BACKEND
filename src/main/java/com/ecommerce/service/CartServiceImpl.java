package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.request.AddItemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartItemService cartItemService;

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
        cartItemService.deleteAllCartItems(cart.getId(), userId);

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
