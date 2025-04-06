package com.webanhang.team_project.controller.common;


import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.cart.ICartItemService;
import com.webanhang.team_project.service.cart.ICartService;
import com.webanhang.team_project.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/cartItems")
public class CartItemController {
    private final ICartItemService cartItemService;
    private final IUserService userService;
    private final ICartService cartService;

    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse> addItemToCart(
            @RequestParam int productId,
            @RequestParam int quantity){
        User user = userService.getUserById(1);
        Cart cart = cartService.initializeNewCartForUser(user);
        cartItemService.addItemToCart(cart.getId(), productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(null, "Item added successfully!" ));
    }

    @DeleteMapping("/cart/{cartId}/item/{itemId}/remove")
    public ResponseEntity<ApiResponse> removeItemFromCart(@PathVariable int cartId,@PathVariable int itemId){
        cartItemService.removeItemFromCart(cartId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Item removed successfully!"));
    }

    @PutMapping("/cart/{cartId}/item/{itemId}/update")
    public ResponseEntity<ApiResponse> updateCartItem(
            @PathVariable int cartId,
            @PathVariable int itemId,
            @RequestParam int quantity){
        cartItemService.updateItemQuantity(cartId, itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success(null, "Item updated successfully!"));
    }
}
