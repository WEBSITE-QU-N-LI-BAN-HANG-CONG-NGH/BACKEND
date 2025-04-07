package com.ecommerce.controller;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Order;
import com.ecommerce.model.PaymentDetail;
import com.ecommerce.model.User;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.PaymentService;
import com.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserService userService;

    /**
     * Tạo URL thanh toán VNPay cho đơn hàng
     * @param jwt JWT token cho xác thực
     * @param orderId ID của đơn hàng cần thanh toán
     * @return URL thanh toán
     */
    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createPayment(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) {
        try {
            // Kiểm tra người dùng và quyền
            User user = userService.findUserByJwt(jwt);
            Order order = orderService.findOrderById(orderId);
            
            // Kiểm tra đơn hàng thuộc về người dùng
            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập đơn hàng này", 
                                     "code", "ORDER_ACCESS_DENIED"));
            }
            
            // Tạo URL thanh toán
            String paymentUrl = paymentService.createPayment(orderId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tạo URL thanh toán thành công",
                "paymentUrl", paymentUrl
            ));
        } catch (GlobalExceptionHandler e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống khi tạo thanh toán", 
                                 "code", "PAYMENT_ERROR",
                                 "message", e.getMessage()));
        }
    }

    /**
     * Xử lý kết quả thanh toán từ VNPay
     * @param params Các tham số nhận được từ VNPay
     * @return Thông tin kết quả thanh toán
     */
    @PostMapping("/vnpay-callback")
    public ResponseEntity<?> vnpayCallback(@RequestParam Map<String, String> params) {
        try {
            PaymentDetail payment = paymentService.processPaymentCallback(params);
            
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            Map<String, Object> response = new HashMap<>();
            
            if ("00".equals(vnp_ResponseCode)) {
                response.put("success", true);
                response.put("message", "Thanh toán thành công");
                response.put("orderId", payment.getOrder().getId());
            } else {
                response.put("success", false);
                response.put("message", "Thanh toán thất bại");
                response.put("responseCode", vnp_ResponseCode);
            }
            
            return ResponseEntity.ok(response);
        } catch (GlobalExceptionHandler e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống khi xử lý kết quả thanh toán", 
                                 "code", "PAYMENT_CALLBACK_ERROR",
                                 "message", e.getMessage()));
        }
    }

    /**
     * Lấy thông tin thanh toán theo ID đơn hàng
     * @param jwt JWT token cho xác thực
     * @param orderId ID của đơn hàng
     * @return Thông tin thanh toán
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) {
        try {
            // Kiểm tra người dùng và quyền
            User user = userService.findUserByJwt(jwt);
            Order order = orderService.findOrderById(orderId);
            
            // Kiểm tra đơn hàng thuộc về người dùng
            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập đơn hàng này", 
                                     "code", "ORDER_ACCESS_DENIED"));
            }
            
            // Lấy thông tin thanh toán
            PaymentDetail payment = order.getPaymentDetails();
            if (payment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy thông tin thanh toán", 
                                     "code", "PAYMENT_NOT_FOUND"));
            }
            
            return ResponseEntity.ok(payment);
        } catch (GlobalExceptionHandler e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống khi lấy thông tin thanh toán", 
                                 "code", "PAYMENT_ERROR",
                                 "message", e.getMessage()));
        }
    }
}
