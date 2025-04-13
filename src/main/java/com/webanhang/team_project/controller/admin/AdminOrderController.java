package com.webanhang.team_project.controller.admin;



import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.service.order.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/orders")
public class AdminOrderController {

    private final IOrderService orderService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders, "Lấy tất cả đơn hàng thành công"));
    }

    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse> confirmOrder(@PathVariable Long orderId) {
        Order order = orderService.confirmedOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Xác nhận đơn hàng thành công"));
    }

    @PutMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse> shipOrder(@PathVariable Long orderId) {
        Order order = orderService.shippedOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Chuyển trạng thái vận chuyển thành công"));
    }

    @PutMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse> deliverOrder(@PathVariable Long orderId) {
        Order order = orderService.deliveredOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Đánh dấu đã giao hàng thành công"));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse> cancelOrder(@PathVariable Long orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Hủy đơn hàng thành công"));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa đơn hàng thành công"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getOrderStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        Map<String, Object> stats = orderService.getOrderStatistics(start, end);
        return ResponseEntity.ok(ApiResponse.success(stats, "Get order statistics success"));
    }
}
