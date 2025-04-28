package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.OrderItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.OrderItemRepository;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportService implements IAdminReportService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Tạo báo cáo sản phẩm trong khoảng thời gian
     *
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách báo cáo sản phẩm
     */
    @Override
    public List<Map<String, Object>> generateProductReports(LocalDate startDate, LocalDate endDate) {
        List<Product> products = productRepository.findAll();
        List<Map<String, Object>> reports = new ArrayList<>();

        for (Product product : products) {
            // Tạo báo cáo cho mỗi sản phẩm
            Map<String, Object> report = new HashMap<>();
            report.put("id", product.getId());
            report.put("name", product.getTitle());
            report.put("category", product.getCategory() != null ? product.getCategory().getName() : "");
            report.put("price", product.getPrice());

            // Sử dụng trường quantitySold thay vì tính toán từ đơn hàng
            long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
            report.put("totalSold", quantitySold);

            // Tính doanh thu từ số lượng đã bán và giá
            int revenue = (int) (product.getDiscountedPrice() * quantitySold);
            report.put("revenue", revenue);

            reports.add(report);
        }

        return reports;
    }


    /**
     * Lấy thống kê tổng quan về sản phẩm
     *
     * @return Map chứa thông tin thống kê
     */
    @Override
    public Map<String, Object> getProductStatistics() {
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
                .filter(p -> p.getNumRatings() >= 4)
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
