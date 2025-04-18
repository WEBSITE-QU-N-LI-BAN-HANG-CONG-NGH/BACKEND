package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.OrderItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.OrderItemRepository;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.service.admin.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * Lấy báo cáo về sản phẩm trong khoảng thời gian
     *
     * @param startDate Ngày bắt đầu khoảng thời gian (định dạng: yyyy-MM-dd)
     * @param endDate Ngày kết thúc khoảng thời gian (định dạng: yyyy-MM-dd)
     * @return Báo cáo về sản phẩm trong khoảng thời gian
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse> getProductReports(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<Map<String, Object>> reports = adminReportService.generateProductReports(start, end);
        return ResponseEntity.ok(ApiResponse.success(reports, "Get product reports success"));
    }

    /**
     * Lấy thống kê tổng quan về sản phẩm
     *
     * @return Thống kê về sản phẩm (tổng số, theo danh mục, đánh giá cao, hết hàng)
     */
    @GetMapping("/products/stats")
    public ResponseEntity<ApiResponse> getProductStats() {
        Map<String, Object> stats = adminReportService.getProductStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Get product statistics success"));
    }

}
