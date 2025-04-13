package com.webanhang.team_project.controller.common;



import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.order.IOrderService;
import com.webanhang.team_project.service.user.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/orders")
public class OrderController {
    private final IOrderService orderService;

    private final  UserService userService;

    @PostMapping("/user/order")
    public ResponseEntity<ApiResponse> placeOrder(@RequestParam int userId){
        Order order = orderService.placeOrder(userId);
        OrderDTO orderDto =  orderService.convertToDto(order);
        return ResponseEntity.ok(ApiResponse.success(orderDto, "Order placed successfully!"));
    }

    @GetMapping("/user/{userId}/order")
    private ResponseEntity<ApiResponse> getUserOrders(@PathVariable int userId){
        List<OrderDTO> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders, "Success!"));
    }

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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred", "code", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<OrderDTO>> userOrderHistory(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        List<Order> orders = orderService.userOrderHistory(user.getId());
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            orderDTOs.add(new OrderDTO(order));
        }
        return new ResponseEntity<>(orderDTOs, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> findOrderById(@PathVariable("id") Long orderId) {
        Order order = orderService.findOrderById(orderId);
        OrderDTO orderDTO = new OrderDTO(order);
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }
}
