package com.webanhang.team_project.controller.admin;



import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.dto.order.OrderDetailDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.service.order.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/orders")
public class AdminOrderController {

    private final IOrderService orderService;

    /**
     * Lấy tất cả đơn hàng trong hệ thống
     *
     * @return Danh sách đơn hàng
     */
    @GetMapping("/all")
    @Transactional
    public ResponseEntity<ApiResponse> getAllOrders() {
        List<Order> orders = orderService.getAllOrdersByJF();
        List<OrderDetailDTO> orderDTOs = orders.stream()
                .map(order -> new OrderDetailDTO(order))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(orderDTOs, "Lấy tất cả đơn hàng thành công"));
    }

    /**
     * Xác nhận đơn hàng
     *
     * @param orderId ID của đơn hàng cần xác nhận
     * @return Thông tin đơn hàng sau khi xác nhận
     */
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse> confirmOrder(@PathVariable Long orderId) {
        Order order = orderService.confirmedOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xác nhận đơn hàng thành công"));
    }

    /**
     * Chuyển đơn hàng sang trạng thái đang giao
     *
     * @param orderId ID của đơn hàng
     * @return Thông tin đơn hàng sau khi cập nhật
     */
    @PutMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse> shipOrder(@PathVariable Long orderId) {
        Order order = orderService.shippedOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Chuyển trạng thái vận chuyển thành công"));
    }

    /**
     * Chuyển đơn hàng sang trạng thái đã giao
     *
     * @param orderId ID của đơn hàng
     * @return Thông tin đơn hàng sau khi cập nhật
     */
    @PutMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse> deliverOrder(@PathVariable Long orderId) {
        Order order = orderService.deliveredOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đánh dấu đã giao hàng thành công"));
    }

    /**
     * Hủy đơn hàng
     *
     * @param orderId ID của đơn hàng cần hủy
     * @return Thông tin đơn hàng sau khi hủy
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse> cancelOrder(@PathVariable Long orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Hủy đơn hàng thành công"));
    }

    /**
     * Xóa đơn hàng
     *
     * @param orderId ID của đơn hàng cần xóa
     * @return Thông báo kết quả
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa đơn hàng thành công"));
    }

    /**
     * Lấy thống kê đơn hàng trong khoảng thời gian
     *
     * @param startDate Ngày bắt đầu (định dạng: yyyy-MM-dd)
     * @param endDate Ngày kết thúc (định dạng: yyyy-MM-dd)
     * @return Thống kê đơn hàng
     */
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
