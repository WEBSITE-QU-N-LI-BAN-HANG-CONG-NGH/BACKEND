package com.webanhang.team_project.controller.common;



import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.order.IOrderService;
import com.webanhang.team_project.service.user.UserService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/user")
    private ResponseEntity<ApiResponse> getUserOrders(@RequestHeader("Authorization") String jwt){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication == null ){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        User user = userService.findUserByJwt(jwt);

        List<Order> orders = orderService.userOrderHistory(user.getId());
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = new OrderDTO(order);
            orderDTOs.add(orderDTO);
        }
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Get history order success!"));
    }

    @PostMapping("/create/{addressId}")
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String jwt,
                                         @PathVariable("addressId") Long addressId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication == null ){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        User user = userService.findUserByJwt(jwt);

        try {
            // Validate if address exists for this user
            boolean addressExists = user.getAddress().stream()
                    .anyMatch(address -> address.getId().equals(addressId));

            if (!addressExists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Address not found", "code", "ADDRESS_NOT_FOUND"));
            }

            Order order = orderService.placeOrder(addressId, user);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to create order", "code", "ORDER_CREATION_FAILED"));
            }
            // Convert Order to OrderDTO
            OrderDTO orderDTO = new OrderDTO(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);
            } catch (Exception e) {
                // Ghi lại lỗi chi tiết vào log của server
                log.error("Error creating order for user {} with addressId {}: {}",
                        (user != null ? user.getId() : "unknown"), addressId, e.getMessage(), e); // Log cả stack trace

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "An unexpected error occurred", "code", "INTERNAL_ERROR"));
            }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> findOrderById(@PathVariable("id") Long orderId) {
        Order order = orderService.findOrderById(orderId);
        OrderDTO orderDTO = new OrderDTO(order);
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }
}
