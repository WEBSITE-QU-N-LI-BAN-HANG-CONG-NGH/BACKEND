package com.webanhang.team_project.controller.customer;



import com.webanhang.team_project.dto.AddItemRequest;
import com.webanhang.team_project.dto.cart.CartDTO;
import com.webanhang.team_project.dto.response.ApiResponse;

import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.cart.CartService;
import com.webanhang.team_project.service.user.UserService;
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
    public ResponseEntity<CartDTO> findUserCart(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        Cart cart = cartService.findUserCart(user.getId());
        CartDTO cartDTO = new CartDTO(cart);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addItemToCart(@RequestHeader("Authorization") String jwt,
                                                     @RequestBody AddItemRequest req) {
        User user = userService.findUserByJwt(jwt);
        cartService.addCartItem(user.getId(), req);

        ApiResponse res = new ApiResponse();
        res.setMessage("Item added to cart successfully");
        res.setStatus(true);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<CartDTO> updateCartItem(@RequestHeader("Authorization") String jwt,
                                                  @PathVariable Long itemId,
                                                  @RequestBody AddItemRequest req) {
        User user = userService.findUserByJwt(jwt);
        Cart cart = cartService.updateCartItem(user.getId(), itemId, req);
        CartDTO cartDTO = new CartDTO(cart);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<ApiResponse> removeCartItem(@RequestHeader("Authorization") String jwt, @PathVariable Long itemId) {
        User user = userService.findUserByJwt(jwt);
        cartService.removeCartItem(user.getId(), itemId);
        
        ApiResponse res = new ApiResponse();
        res.setMessage("Item removed from cart successfully");
        res.setStatus(true);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clearCart(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        cartService.clearCart(user.getId());
        
        ApiResponse res = new ApiResponse();
        res.setMessage("Cart cleared successfully");
        res.setStatus(true);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
