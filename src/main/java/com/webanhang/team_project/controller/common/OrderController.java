package com.webanhang.team_project.controller.common;



import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.security.otp.OtpService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/orders")
public class OrderController {
    private final IOrderService orderService;

    private final  UserService userService;

    private final OtpService otpService;

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

            // Create orders (one per seller)
            List<Order> orders = orderService.placeOrder(addressId, user);
            if (orders == null || orders.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to create orders", "code", "ORDER_CREATION_FAILED"));
            }

            // Convert orders to DTOs
            List<OrderDTO> orderDTOs = orders.stream()
                    .map(OrderDTO::new)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("orders", orderDTOs);
            response.put("totalOrders", orders.size());
            response.put("totalAmount", orders.stream()
                    .mapToInt(order -> order.getTotalDiscountedPrice() != null ? order.getTotalDiscountedPrice() : 0)
                    .sum());
            response.put("message", "Orders created successfully. Total " + orders.size() + " orders from different sellers.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse> getPendingOrders() {
        List<Order> orders = orderService.getPendingOrders();
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = new OrderDTO(order);
            orderDTOs.add(orderDTO);
        }
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Get pending orders success!"));
    }

    @GetMapping("/confirmed")
    public ResponseEntity<ApiResponse> getConfirmedOrders() {
        List<Order> orders = orderService.getConfirmedOrders();
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = new OrderDTO(order);
            orderDTOs.add(orderDTO);
        }
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Get confirmed orders success!"));
    }

    @GetMapping("/shipped")
    public ResponseEntity<ApiResponse> getShippedOrders() {
        List<Order> orders = orderService.getShippedOrders();
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = new OrderDTO(order);
            orderDTOs.add(orderDTO);
        }
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Get shipped orders success!"));
    }

    @GetMapping("/delivered")
    public ResponseEntity<ApiResponse> getDeliveredOrders() {
        List<Order> orders = orderService.getDeliveredOrders();
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = new OrderDTO(order);
            orderDTOs.add(orderDTO);
        }
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Get delivered orders success!"));
    }

    @GetMapping("/cancelled")
    public ResponseEntity<ApiResponse> getCancelledOrders() {
        List<Order> orders = orderService.getCancelledOrders();
        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = new OrderDTO(order);
            orderDTOs.add(orderDTO);
        }
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Get cancelled orders success!"));
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<ApiResponse> cancelOrder(@PathVariable("id") Long orderId) {
        Order order = orderService.cancelOrder(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order not found"));
        }
        OrderDTO orderDTO = new OrderDTO(order);
        return ResponseEntity.ok(ApiResponse.success(orderDTO, "Order cancelled successfully"));
    }


    @PostMapping("/send-mail/{orderId}")
    public ResponseEntity<ApiResponse> sendMail(@RequestHeader("Authorization") String jwt,
                                                @PathVariable("orderId") Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        User user = userService.findUserByJwt(jwt);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found"));
        }

        try {
            Order order = orderService.findOrderById(orderId);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order not found"));
            }

            // Kiểm tra xem đơn hàng có thuộc về user hiện tại không
            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You don't have permission to access this order"));
            }

            otpService.sendOrderMail(user.getEmail(), order);
            return ResponseEntity.ok(ApiResponse.success(null, "Email sent successfully"));
        } catch (Exception e) {
            log.error("Error sending email for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }
}
