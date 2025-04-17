package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.OrderItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.OrderItemRepository;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
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

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

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

        List<Map<String, Object>> reports = generateProductReports(start, end);
        return ResponseEntity.ok(ApiResponse.success(reports, "Get product reports success"));
    }

    /**
     * Lấy thống kê tổng quan về sản phẩm
     *
     * @return Thống kê về sản phẩm (tổng số, theo danh mục, đánh giá cao, hết hàng)
     */
    @GetMapping("/products/stats")
    public ResponseEntity<ApiResponse> getProductStats() {
        Map<String, Object> stats = getProductStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Get product statistics success"));
    }

    /**
     * Tạo báo cáo sản phẩm trong khoảng thời gian
     *
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách báo cáo sản phẩm
     */
    private List<Map<String, Object>> generateProductReports(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Lấy các đơn hàng trong khoảng thời gian
        List<Order> orders = orderRepository.findByOrderDateBetween(start, end);

        // Tạo map để theo dõi báo cáo cho mỗi sản phẩm
        Map<Long, Map<String, Object>> productReports = new HashMap<>();

        // Duyệt qua các đơn hàng và cập nhật báo cáo sản phẩm
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    Long productId = product.getId();

                    // Tạo báo cáo mới nếu chưa có
                    if (!productReports.containsKey(productId)) {
                        Map<String, Object> report = new HashMap<>();
                        report.put("id", productId);
                        report.put("name", product.getTitle());
                        report.put("category", product.getCategory() != null ? product.getCategory().getName() : "");
                        report.put("price", product.getPrice());
                        report.put("totalSold", 0);
                        report.put("revenue", 0);
                        report.put("returns", 0);

                        productReports.put(productId, report);
                    }

                    // Cập nhật thông tin báo cáo
                    Map<String, Object> report = productReports.get(productId);

                    // Cập nhật số lượng bán và doanh thu
                    int currentSold = (int) report.get("totalSold");
                    int currentRevenue = (int) report.get("revenue");

                    report.put("totalSold", currentSold + item.getQuantity());
                    report.put("revenue", currentRevenue + (item.getPrice() * item.getQuantity()));
                }
            }
        }

        // Chuyển map thành list để trả về
        return new ArrayList<>(productReports.values());
    }

    /**
     * Lấy thống kê tổng quan về sản phẩm
     *
     * @return Map chứa thông tin thống kê
     */
    private Map<String, Object> getProductStatistics() {
        Map<String, Object> result = new HashMap<>();

        // Tổng số báo cáo
        List<Product> products = productRepository.findAll();
        result.put("totalProducts", products.size());

        // Số lượng sản phẩm theo danh mục
        Map<String, Integer> productsByCategory = new HashMap<>();

        for (Product product : products) {
            if (product.getCategory() != null) {
                String category = product.getCategory().getName();
                productsByCategory.put(category, productsByCategory.getOrDefault(category, 0) + 1);
            }
        }

        result.put("productsByCategory", productsByCategory);

        // Số lượng sản phẩm có rating cao
        long highRatedProducts = products.stream()
                .filter(p -> p.getNumRating() >= 4)
                .count();

        result.put("highRatedProducts", highRatedProducts);

        // Số lượng sản phẩm hết hàng
        long outOfStockProducts = products.stream()
                .filter(p -> p.getQuantity() <= 0)
                .count();

        result.put("outOfStockProducts", outOfStockProducts);

        return result;
    }
}
