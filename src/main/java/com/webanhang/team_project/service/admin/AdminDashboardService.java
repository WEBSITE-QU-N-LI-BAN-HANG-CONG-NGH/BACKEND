package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.seller.SellerRevenueDTO;
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
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService implements IAdminDashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public BigDecimal totalMonthInCome() {
        LocalDate now = LocalDate.now();
        LocalDateTime startDate = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);

        List<Order> monthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                startDate,
                endDate,
                OrderStatus.DELIVERED);

        return monthOrders.stream()
                .map(Order::getTotalAmount)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal compareToRecentMonthIncomeByPercent() {
        BigDecimal currentMonthIncome = totalMonthInCome();
        BigDecimal lastMonthIncome = getLastMonthIncome();

        // Tránh chia cho 0
        if (lastMonthIncome.compareTo(BigDecimal.ZERO) == 0) {
            return currentMonthIncome.compareTo(BigDecimal.ZERO) > 0 ?
                    new BigDecimal(100) : BigDecimal.ZERO;
        }

        return currentMonthIncome
                .subtract(lastMonthIncome)
                .multiply(new BigDecimal(100))
                .divide(lastMonthIncome, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal compareToRecentMonthIncomeByVND() {
        BigDecimal currentMonthIncome = totalMonthInCome();
        BigDecimal lastMonthIncome = getLastMonthIncome();

        return currentMonthIncome.subtract(lastMonthIncome);
    }

    @Override
    public List<SellerRevenueDTO> getTopSellers(int limit) {
        // Lấy tất cả người dùng có vai trò SELLER
        List<User> sellers = userRepository.findAll();
        List<SellerRevenueDTO> sellerStats = new ArrayList<>();
        List<Order> completedOrders = orderRepository.findByOrderStatus(OrderStatus.DELIVERED);

        // Tạo map để theo dõi doanh thu của mỗi người bán
        Map<Long, BigDecimal> sellerRevenue = new HashMap<>();
        Map<Long, Integer> sellerOrderCount = new HashMap<>();

        // Tính toán doanh thu từ các đơn hàng
        for (Order order : completedOrders) {
            if (order.getUser() != null) {
                Long sellerId = order.getUser().getId();
                BigDecimal orderAmount = BigDecimal.valueOf(order.getTotalAmount());

                // Cập nhật doanh thu và số lượng đơn hàng
                sellerRevenue.put(sellerId, sellerRevenue.getOrDefault(sellerId, BigDecimal.ZERO).add(orderAmount));
                sellerOrderCount.put(sellerId, sellerOrderCount.getOrDefault(sellerId, 0) + 1);
            }
        }

        // Tạo DTO cho mỗi người bán
        for (User seller : sellers) {
            Long sellerId = seller.getId();
            if (sellerRevenue.containsKey(sellerId)) {
                SellerRevenueDTO dto = new SellerRevenueDTO(
                        sellerId,
                        seller.getLastName(),
                        sellerRevenue.get(sellerId),
                        sellerOrderCount.get(sellerId),
                        0.0 // Có thể tính toán tỷ lệ tăng trưởng nếu cần
                );
                sellerStats.add(dto);
            }
        }

        return sellerStats.stream()
                .sorted(Comparator.comparing(SellerRevenueDTO::getTotalRevenue).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public Map<String, BigDecimal> getRevenueDistribution() {
        // Thực hiện phân tích doanh thu theo danh mục
        List<Product> products = productRepository.findAll();
        Map<String, BigDecimal> distribution = new HashMap<>();

        // Nhóm sản phẩm theo danh mục và tính tổng doanh thu
        for (Product product : products) {
            String category = product.getCategory() != null ? product.getCategory().getName() : "Khác";
            long sold = product.getQuantitySold() != null ? product.getQuantitySold() : 0;
            BigDecimal revenue = BigDecimal.valueOf(product.getDiscountedPrice() * sold);

            distribution.put(category, distribution.getOrDefault(category, BigDecimal.ZERO).add(revenue));
        }

        // Tính tổng doanh thu
        BigDecimal totalRevenue = distribution.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Chuyển đổi thành phần trăm
        Map<String, BigDecimal> percentages = new HashMap<>();
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : distribution.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .multiply(new BigDecimal(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP);
                percentages.put(entry.getKey(), percentage);
            }
        } else {
            // Nếu không có doanh thu, phân phối đều
            for (String category : distribution.keySet()) {
                percentages.put(category, BigDecimal.valueOf(100.0 / distribution.size()));
            }
        }

        return percentages;
    }

    @Override
    public Map<String, Object> getMonthlyRevenue() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> monthlyData = new ArrayList<>();

        // Lấy dữ liệu 7 tháng gần nhất
        LocalDate now = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);

            List<Order> monthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                    startOfMonth, endOfMonth, OrderStatus.DELIVERED);

            BigDecimal revenue = monthOrders.stream()
                    .map(order -> BigDecimal.valueOf(order.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profit = revenue.multiply(new BigDecimal("0.25")); // Giả định lợi nhuận là 25% doanh thu

            int totalOrders = monthOrders.size();

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month.getMonthValue());
            monthData.put("year", month.getYear());
            monthData.put("revenue", revenue);
            monthData.put("profit", profit);
            monthData.put("orders", totalOrders);

            // Tính tỷ lệ tăng trưởng nếu có tháng trước
            if (i < 6) {
                Map<String, Object> prevMonth = monthlyData.get(monthlyData.size() - 1);
                BigDecimal prevRevenue = (BigDecimal) prevMonth.get("revenue");

                if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal growthRate = revenue.subtract(prevRevenue)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(prevRevenue, 2, RoundingMode.HALF_UP);
                    monthData.put("growth", growthRate);
                } else {
                    monthData.put("growth", BigDecimal.ZERO);
                }
            } else {
                monthData.put("growth", BigDecimal.ZERO);
            }

            monthlyData.add(monthData);
        }

        result.put("monthlyData", monthlyData);
        return result;
    }

    private BigDecimal getLastMonthIncome() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDateTime startDate = lastMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(23, 59, 59);

        List<Order> lastMonthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                startDate,
                endDate,
                OrderStatus.DELIVERED);

        return lastMonthOrders.stream()
                .map(Order::getTotalAmount)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}