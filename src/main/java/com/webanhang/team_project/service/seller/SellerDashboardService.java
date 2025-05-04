package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.dto.seller.OrderStatsDTO;
import com.webanhang.team_project.dto.seller.SellerDashboardDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SellerDashboardService implements ISellerDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardDTO getDashboardData(Long sellerId) {
        // Kiểm tra người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Tính toán các thông số
        BigDecimal totalRevenue = calculateTotalRevenue(sellerId);
        Integer totalOrders = countTotalOrders(sellerId);
        Integer totalProducts = countTotalProducts(sellerId);

        // Lấy đơn hàng gần đây
        List<OrderStatsDTO> recentOrders = getRecentOrders(sellerId);

        // Doanh thu theo tuần
        Map<String, BigDecimal> revenueByWeek = getRevenueByWeek(sellerId);

        // Doanh thu theo tháng
        Map<String, BigDecimal> revenueByMonth = getRevenueByMonth(sellerId);

        return new SellerDashboardDTO(
                totalRevenue,
                totalOrders,
                totalProducts,
                recentOrders,
                revenueByWeek,
                revenueByMonth
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMonthlyRevenue(Long sellerId) {
        Map<String, BigDecimal> revenueData = new LinkedHashMap<>();
        List<Product> sellerProducts = productRepository.findBySellerId(sellerId);

        // Lấy dữ liệu 12 tháng gần nhất
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthLabel = month.format(formatter);

            // Tính doanh thu tháng
            BigDecimal revenue = BigDecimal.ZERO;

            for (Product product : sellerProducts) {
                long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
                revenue = revenue.add(BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold));
            }

            revenueData.put(monthLabel, revenue);
        }

        return revenueData;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getRevenueByWeek(Long sellerId) {
        Map<String, BigDecimal> revenueByWeek = new LinkedHashMap<>();
        List<Product> sellerProducts = productRepository.findBySellerId(sellerId);

        // Lấy dữ liệu 6 tuần gần nhất
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 5; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            String weekLabel = weekStart.format(formatter) + " - " + weekEnd.format(formatter);

            // Tính doanh thu tuần
            BigDecimal revenue = BigDecimal.ZERO;

            for (Product product : sellerProducts) {
                long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
                BigDecimal productRevenue = BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold);
                revenue = revenue.add(productRevenue);
            }

            revenueByWeek.put(weekLabel, revenue);
        }

        return revenueByWeek;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getOrderStats(Long sellerId) {
        Map<String, Integer> orderStats = new HashMap<>();

        // Đếm số lượng đơn hàng theo trạng thái
        List<Order> sellerOrders = orderRepository.findByUserId(sellerId);

        int pendingCount = 0;
        int confirmedCount = 0;
        int shippedCount = 0;
        int deliveredCount = 0;
        int cancelledCount = 0;

        for (Order order : sellerOrders) {
            switch (order.getOrderStatus()) {
                case PENDING:
                    pendingCount++;
                    break;
                case CONFIRMED:
                    confirmedCount++;
                    break;
                case SHIPPED:
                    shippedCount++;
                    break;
                case DELIVERED:
                    deliveredCount++;
                    break;
                case CANCELLED:
                    cancelledCount++;
                    break;
            }
        }

        orderStats.put("pending", pendingCount);
        orderStats.put("confirmed", confirmedCount);
        orderStats.put("shipped", shippedCount);
        orderStats.put("delivered", deliveredCount);
        orderStats.put("cancelled", cancelledCount);
        orderStats.put("total", sellerOrders.size());

        return orderStats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getProductStats(Long sellerId) {
        Map<String, Integer> productStats = new HashMap<>();

        // Đếm tổng số sản phẩm và trạng thái tồn kho
        List<Product> sellerProducts = productRepository.findBySellerId(sellerId);

        int inStock = 0;
        int outOfStock = 0;
        int lowStock = 0;  // Dưới 5 sản phẩm

        for (Product product : sellerProducts) {
            if (product.getQuantity() <= 0) {
                outOfStock++;
            } else if (product.getQuantity() < 5) {
                lowStock++;
            } else {
                inStock++;
            }
        }

        productStats.put("total", sellerProducts.size());
        productStats.put("inStock", inStock);
        productStats.put("outOfStock", outOfStock);
        productStats.put("lowStock", lowStock);

        return productStats;
    }

    // Các phương thức hỗ trợ
    @Transactional(readOnly = true)
    private BigDecimal calculateTotalRevenue(Long sellerId) {
        // Lấy tất cả sản phẩm của người bán
        List<Product> sellerProducts = productRepository.findBySellerId(sellerId);

        // Tính tổng doanh thu dựa trên số lượng đã bán và giá
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Product product : sellerProducts) {
            long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
            BigDecimal productRevenue = BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold);
            totalRevenue = totalRevenue.add(productRevenue);
        }

        return totalRevenue;
    }

    @Transactional(readOnly = true)
    private Integer countTotalOrders(Long sellerId) {
        List<Order> orders = orderRepository.findByUserId(sellerId);
        return orders.size();
    }

    @Transactional(readOnly = true)
    private Integer countTotalProducts(Long sellerId) {
        return productRepository.findBySellerId(sellerId).size();
    }

    @Transactional(readOnly = true)
    private List<OrderStatsDTO> getRecentOrders(Long sellerId) {
        List<Order> orders = orderRepository.findByUserId(sellerId);

        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .limit(5)
                .map(order -> new OrderStatsDTO(
                        order.getId(),
                        order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                        order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        order.getTotalDiscountedPrice(),
                        order.getOrderStatus().name()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    private Map<String, BigDecimal> getRevenueByMonth(Long sellerId) {
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        List<Product> sellerProducts = productRepository.findBySellerId(sellerId);

        // Lấy dữ liệu 6 tháng gần nhất
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthLabel = month.format(formatter);

            // Tính doanh thu tháng dựa trên số lượng đã bán của sản phẩm
            BigDecimal revenue = BigDecimal.ZERO;

            for (Product product : sellerProducts) {
                long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
                BigDecimal productRevenue = BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold);
                revenue = revenue.add(productRevenue);
            }

            revenueByMonth.put(monthLabel, revenue);
        }

        return revenueByMonth;
    }
}