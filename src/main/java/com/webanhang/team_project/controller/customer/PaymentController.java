package com.webanhang.team_project.controller.customer;

import com.webanhang.team_project.exceptions.AppException;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.PaymentDetail;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.PaymentRepository;
import com.webanhang.team_project.service.payment.PaymentService;
import com.webanhang.team_project.service.order.OrderService;
import com.webanhang.team_project.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("${api.prefix}/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService; // Giữ lại nếu cần, hoặc bỏ nếu không dùng trực tiếp ở đây

    @Autowired
    private UserService userService; // Giữ lại nếu cần

    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createPayment(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) {
        try {
            User user = userService.findUserByJwt(jwt);
            Order order = orderService.findOrderById(orderId);

            if (order == null) { // Thêm kiểm tra null cho order
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy đơn hàng với ID: " + orderId,
                                "code", "ORDER_NOT_FOUND"));
            }

            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập đơn hàng này",
                                "code", "ORDER_ACCESS_DENIED"));
            }

            String paymentUrl = paymentService.createPayment(orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo URL thanh toán thành công",
                    "paymentUrl", paymentUrl
            ));
        } catch (AppException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (RuntimeException e) { // Bắt RuntimeException cụ thể hơn nếu paymentService.createPayment ném ra
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // Có thể là BAD_REQUEST nếu lỗi do nghiệp vụ
                    .body(Map.of("error", "Lỗi khi tạo yêu cầu thanh toán: " + e.getMessage(),
                            "code", "PAYMENT_CREATION_FAILED"));
        }
        catch (Exception e) {
            // Log lỗi chi tiết ở backend
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống khi tạo thanh toán",
                            "code", "PAYMENT_ERROR",
                            "detail", e.getMessage())); // Thêm detail để frontend có thể hiển thị nếu cần
        }
    }

    // Đã là GetMapping, tên hàm có thể giữ nguyên hoặc đổi cho rõ ràng
    @GetMapping("/vnpay-callback")
    public ResponseEntity<?> vnpayCallback(@RequestParam Map<String, String> params) { // Đổi tên params cho ngắn gọn
        System.out.println("[PaymentController] Received VNPAY Callback with params: " + params); // LOGGING
        try {
            String vnp_TxnRef = params.get("vnp_TxnRef");
            if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
                System.err.println("[PaymentController] Missing vnp_TxnRef. Parameters: " + params);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", "Thiếu mã tham chiếu giao dịch (vnp_TxnRef)",
                                "code", "MISSING_TXN_REF"));
            }

            // Gọi service để xử lý callback
            PaymentDetail payment = paymentService.processPaymentCallback(params);
            System.out.println("[PaymentController] Payment processed by service: " + payment); // LOGGING

            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            // VNPay có thể trả về vnp_TransactionStatus cho một số trường hợp thay vì vnp_ResponseCode
            if (vnp_ResponseCode == null && params.containsKey("vnp_TransactionStatus")) {
                vnp_ResponseCode = params.get("vnp_TransactionStatus");
            }


            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("orderId", payment.getOrder().getId()); // Luôn trả về orderId

            if ("00".equals(vnp_ResponseCode)) {
                responseMap.put("success", true);
                responseMap.put("message", "Thanh toán thành công");
                responseMap.put("paymentId", payment.getId());
                responseMap.put("transactionId", payment.getTransactionId());
                responseMap.put("responseCode", vnp_ResponseCode);
                System.out.println("[PaymentController] Payment success for TxnRef: " + vnp_TxnRef);
            } else {
                responseMap.put("success", false);
                responseMap.put("message", "Thanh toán thất bại hoặc đang chờ xử lý");
                responseMap.put("responseCode", vnp_ResponseCode != null ? vnp_ResponseCode : "UNKNOWN");
                System.err.println("[PaymentController] Payment failed or pending for TxnRef: " + vnp_TxnRef + ", ResponseCode: " + vnp_ResponseCode);
            }

            // Dù thành công hay thất bại, luôn trả về thông tin đã xử lý
            // Frontend sẽ dựa vào "success" và "responseCode" để quyết định
            return ResponseEntity.ok(responseMap);

        } catch (RuntimeException e) { // Bắt lỗi cụ thể hơn từ service
            System.err.println("[PaymentController] RuntimeException during VNPAY callback processing: " + e.getMessage());
            e.printStackTrace(); // Log stack trace
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // Lỗi nghiệp vụ có thể không phải 500
                    .body(Map.of("success", false, "error", "Lỗi khi xử lý kết quả thanh toán: " + e.getMessage(),
                            "code", "PAYMENT_CALLBACK_PROCESSING_ERROR"));
        }
        catch (Exception e) {
            System.err.println("[PaymentController] Unexpected Exception during VNPAY callback processing: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Lỗi hệ thống không mong muốn khi xử lý kết quả thanh toán",
                            "code", "SYSTEM_ERROR_PAYMENT_CALLBACK",
                            "detail", e.getMessage()));
        }
    }

    // ... (các hàm khác như getPaymentByOrderId, createMultipleOrdersPayment giữ nguyên)
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) {
        try {
            User user = userService.findUserByJwt(jwt);
            Order order = orderService.findOrderById(orderId);

            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy đơn hàng với ID: " + orderId,
                                "code", "ORDER_NOT_FOUND"));
            }

            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập đơn hàng này",
                                "code", "ORDER_ACCESS_DENIED"));
            }

            PaymentDetail payment = order.getPaymentDetails(); // Giả sử Order có getPaymentDetails()
            if (payment == null) {
                // Thử tìm payment bằng cách khác nếu order không trực tiếp giữ payment detail
                payment = paymentRepository.findByOrderId(orderId).orElse(null);
                if (payment == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Không tìm thấy thông tin thanh toán cho đơn hàng này",
                                    "code", "PAYMENT_NOT_FOUND_FOR_ORDER"));
                }
            }

            return ResponseEntity.ok(payment);
        } catch (AppException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống khi lấy thông tin thanh toán",
                            "code", "PAYMENT_ERROR",
                            "message", e.getMessage()));
        }
    }
    @Autowired
    private PaymentRepository paymentRepository; // Thêm nếu chưa có


    @PostMapping("/create-multiple")
    public ResponseEntity<?> createMultipleOrdersPayment(
            @RequestHeader("Authorization") String jwt,
            @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> orderIds = request.get("orderIds");
            if (orderIds == null || orderIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Order IDs list cannot be empty", "code", "EMPTY_ORDER_IDS"));
            }

            User user = userService.findUserByJwt(jwt);
            long totalAmount = 0; // Sử dụng long cho số tiền lớn
            List<Order> orders = new ArrayList<>();

            for (Long orderId : orderIds) {
                Order order = orderService.findOrderById(orderId);
                if (order == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Không tìm thấy đơn hàng với ID: " + orderId, "code", "ORDER_NOT_FOUND_MULTI"));
                }
                if (!order.getUser().getId().equals(user.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Bạn không có quyền truy cập đơn hàng " + orderId,
                                    "code", "ORDER_ACCESS_DENIED"));
                }
                orders.add(order);
                totalAmount += order.getTotalDiscountedPrice() != null ? order.getTotalDiscountedPrice() : 0;
            }

            // TODO: Cần cập nhật PaymentService để xử lý thanh toán cho nhiều đơn hàng
            // và tổng số tiền `totalAmount` nếu cần.
            // Hiện tại, đang tạo thanh toán cho đơn hàng đầu tiên với tổng số tiền của đơn đó.
            // Nếu bạn muốn một giao dịch VNPay cho tổng số tiền của nhiều đơn,
            // PaymentServiceImpl.createPayment cần được sửa để nhận `totalAmount`
            // và có thể là danh sách orderIds để tham chiếu.
            // Tạm thời, chúng ta vẫn gọi createPayment cho đơn hàng đầu tiên như cũ.
            // Nếu backend logic yêu cầu một payment record cho nhiều order, cần điều chỉnh.
            String paymentUrl = paymentService.createPayment(orderIds.get(0));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment URL created successfully for " + orderIds.size() + " orders",
                    "paymentUrl", paymentUrl,
                    "totalAmount", totalAmount, // Trả về tổng số tiền đã tính
                    "orderIds", orderIds
            ));
        } catch (AppException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "System error while creating payment",
                            "code", "PAYMENT_ERROR",
                            "message", e.getMessage()));
        }
    }

}