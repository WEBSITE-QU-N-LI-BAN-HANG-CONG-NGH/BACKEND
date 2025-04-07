package com.ecommerce.controller;

import com.ecommerce.DTO.CartDTO;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Cart;
import com.ecommerce.model.User;
import com.ecommerce.request.AddItemRequest;
import com.ecommerce.response.ApiResponse;
import com.ecommerce.service.CartService;
import com.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public ResponseEntity<CartDTO> findUserCart(@RequestHeader("Authorization") String jwt) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        Cart cart = cartService.findUserCart(user.getId());
        CartDTO cartDTO = new CartDTO(cart);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addItemToCart(@RequestHeader("Authorization") String jwt, 
            @RequestBody AddItemRequest req) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        cartService.addCartItem(user.getId(), req);

        ApiResponse res = new ApiResponse();
        res.setMessage("Item added to cart successfully");
        res.setStatus(true);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<CartDTO> updateCartItem(@RequestHeader("Authorization") String jwt,
            @PathVariable Long itemId, @RequestBody AddItemRequest req) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        Cart cart = cartService.updateCartItem(user.getId(), itemId, req);
        CartDTO cartDTO = new CartDTO(cart);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<ApiResponse> removeCartItem(@RequestHeader("Authorization") String jwt,
            @PathVariable Long itemId) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        cartService.removeCartItem(user.getId(), itemId);
        
        ApiResponse res = new ApiResponse();
        res.setMessage("Item removed from cart successfully");
        res.setStatus(true);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clearCart(@RequestHeader("Authorization") String jwt) 
            throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        cartService.clearCart(user.getId());
        
        ApiResponse res = new ApiResponse();
        res.setMessage("Cart cleared successfully");
        res.setStatus(true);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
