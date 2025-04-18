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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerDashboardService implements ISellerDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public SellerDashboardDTO getDashboardData(Long sellerId) {
        // Kiểm tra người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Tính toán các thông số
        BigDecimal totalRevenue = calculateTotalRevenue(sellerId);
        BigDecimal lastMonthRevenue = calculateLastMonthRevenue(sellerId);
        BigDecimal revenueChange = calculateRevenueChange(totalRevenue, lastMonthRevenue);

        Integer totalOrders = countTotalOrders(sellerId);
        Integer lastMonthOrders = countLastMonthOrders(sellerId);
        Integer ordersChange = calculateOrdersChange(totalOrders, lastMonthOrders);

        Integer totalProducts = countTotalProducts(sellerId);

        // Lấy đơn hàng gần đây
        List<OrderStatsDTO> recentOrders = getRecentOrders(sellerId);

        // Doanh thu theo tháng
        Map<String, BigDecimal> revenueByMonth = getRevenueByMonth(sellerId);

        return new SellerDashboardDTO(
                totalRevenue,
                totalOrders,
                totalProducts,
                revenueChange,
                ordersChange,
                recentOrders,
                revenueByMonth
        );
    }

    @Override
    public Map<String, BigDecimal> getMonthlyRevenue(Long sellerId) {
        Map<String, BigDecimal> revenueData = new LinkedHashMap<>();
        List<Product> sellerProducts = getSellerProducts(sellerId);

        // Lấy dữ liệu 12 tháng gần nhất
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthLabel = month.format(formatter);

            // Tính doanh thu tháng
            LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);

            BigDecimal revenue = BigDecimal.ZERO;

            for (Product product : sellerProducts) {
                long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
                revenue = revenue.add(BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold));
            }

            revenueData.put(monthLabel, revenue);
        }

        return revenueData;
    }

    @Override
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
    public Map<String, Integer> getProductStats(Long sellerId) {
        Map<String, Integer> productStats = new HashMap<>();

        // Đếm tổng số sản phẩm
        List<Product> products = productRepository.findAll();
        List<Product> sellerProducts = products.stream()
                .filter(product -> product.getCategory() != null &&
                        product.getCategory().getProducts() != null &&
                        product.getCategory().getProducts().stream()
                                .anyMatch(p -> p.getId().equals(sellerId)))
                .collect(Collectors.toList());

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
    private BigDecimal calculateTotalRevenue(Long sellerId) {
        // Lấy tất cả sản phẩm của người bán
        List<Product> sellerProducts = getSellerProducts(sellerId);

        // Tính tổng doanh thu dựa trên số lượng đã bán và giá
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Product product : sellerProducts) {
            long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
            BigDecimal productRevenue = BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold);
            totalRevenue = totalRevenue.add(productRevenue);
        }

        return totalRevenue;
    }

    private BigDecimal calculateLastMonthRevenue(Long sellerId) {
        // Giả định doanh thu tháng trước là 80% doanh thu hiện tại
        // Trong thực tế, ta cần lưu lịch sử doanh thu theo tháng trong cơ sở dữ liệu
        return calculateTotalRevenue(sellerId).multiply(new BigDecimal("0.8"));
    }

    private BigDecimal calculateRevenueChange(BigDecimal currentRevenue, BigDecimal lastRevenue) {
        if (lastRevenue.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return currentRevenue.subtract(lastRevenue)
                .multiply(new BigDecimal(100))
                .divide(lastRevenue, 2, RoundingMode.HALF_UP);
    }

    private Integer countTotalOrders(Long sellerId) {
        List<Order> orders = orderRepository.findByUserId(sellerId);
        return orders.size();
    }

    private Integer countLastMonthOrders(Long sellerId) {
        LocalDate now = LocalDate.now();
        LocalDate lastMonth = now.minusMonths(1);
        LocalDateTime startOfLastMonth = lastMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(23, 59, 59);

        List<Order> lastMonthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                startOfLastMonth, endOfLastMonth, null);

        return (int) lastMonthOrders.stream()
                .filter(order -> order.getUser().getId().equals(sellerId))
                .count();
    }

    private Integer calculateOrdersChange(Integer currentOrders, Integer lastOrders) {
        if (lastOrders == 0) {
            return 0;
        }
        return (currentOrders - lastOrders) * 100 / lastOrders;
    }

    private Integer countTotalProducts(Long sellerId) {
        return getSellerProducts(sellerId).size();
    }

    private List<OrderStatsDTO> getRecentOrders(Long sellerId) {
        List<Order> orders = orderRepository.findByUserId(sellerId);

        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .limit(5)
                .map(order -> new OrderStatsDTO(
                        order.getId(),
                        order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                        order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        order.getTotalAmount(),
                        order.getOrderStatus().name()
                ))
                .toList();
    }

    private Map<String, BigDecimal> getRevenueByMonth(Long sellerId) {
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        List<Product> sellerProducts = getSellerProducts(sellerId);

        // Lấy dữ liệu 6 tháng gần nhất
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthLabel = month.format(formatter);

            // Tính doanh thu tháng
            LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);

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

    private List<Product> getSellerProducts(Long sellerId) {
        List<Product> allProducts = productRepository.findAll();

        return allProducts.stream()
                .filter(product -> product.getCategory() != null &&
                        product.getCategory().getProducts() != null &&
                        product.getCategory().getProducts().stream()
                                .anyMatch(p -> p.getId().equals(sellerId)))
                .toList();
    }
}
