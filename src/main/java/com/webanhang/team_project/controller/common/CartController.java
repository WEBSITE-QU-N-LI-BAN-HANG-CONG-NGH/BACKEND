package com.webanhang.team_project.controller.common;



import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.cart.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/carts")
public class CartController {
    private final ICartService cartService;

    @GetMapping("/user/{userId}/cart")
    public ResponseEntity<ApiResponse> getUserCart(@PathVariable int userId){
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Success"));
    }

    @DeleteMapping("/cart/{cartId}/clear")
    public void clearCart(@PathVariable int cartId){
        cartService.clearCart(cartId);
    }
}
