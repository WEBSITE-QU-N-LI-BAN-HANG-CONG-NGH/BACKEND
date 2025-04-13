//package com.webanhang.team_project.controller.common;
//
//
//
//import com.webanhang.team_project.dto.order.OrderDTO;
//import com.webanhang.team_project.model.Order;
//import com.webanhang.team_project.dto.response.ApiResponse;
//import com.webanhang.team_project.service.order.IOrderService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("${api.prefix}/orders")
//public class OrderController {
//    private final IOrderService orderService;
//
//    @PostMapping("/user/order")
//    public ResponseEntity<ApiResponse> placeOrder(@RequestParam int userId){
//        Order order = orderService.placeOrder(userId);
//        OrderDTO orderDto =  orderService.convertToDto(order);
//        return ResponseEntity.ok(ApiResponse.success(orderDto, "Order placed successfully!"));
//    }
//
//    @GetMapping("/user/{userId}/order")
//    private ResponseEntity<ApiResponse> getUserOrders(@PathVariable int userId){
//        List<OrderDTO> orders = orderService.getUserOrders(userId);
//        return ResponseEntity.ok(ApiResponse.success(orders, "Success!"));
//    }
//}
