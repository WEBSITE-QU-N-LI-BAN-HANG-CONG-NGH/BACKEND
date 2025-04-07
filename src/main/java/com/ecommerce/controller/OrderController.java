package com.ecommerce.controller;

import com.ecommerce.DTO.OrderDTO;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;
import com.ecommerce.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @PostMapping("/create/{addressId}")
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String jwt,
                                         @PathVariable("addressId") Long addressId) {
        try {
            User user = userService.findUserByJwt(jwt);

            // Validate if address exists for this user
            boolean addressExists = user.getAddress().stream()
                    .anyMatch(address -> address.getId().equals(addressId));

            if (!addressExists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Address not found", "code", "ADDRESS_NOT_FOUND"));
            }

            Order order = orderService.placeOrder(addressId, user);
            OrderDTO orderDTO = new OrderDTO(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);

        } catch (GlobalExceptionHandler e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred", "code", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<OrderDTO>> userOrderHistory(@RequestHeader("Authorization") String jwt) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        List<Order> orders = orderService.userOrderHistory(user.getId());
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            orderDTOs.add(new OrderDTO(order));
        }
        return new ResponseEntity<>(orderDTOs, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> findOrderById(@PathVariable("id") Long orderId) throws GlobalExceptionHandler {
        Order order = orderService.findOrderById(orderId);
        OrderDTO orderDTO = new OrderDTO(order);
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }
}
