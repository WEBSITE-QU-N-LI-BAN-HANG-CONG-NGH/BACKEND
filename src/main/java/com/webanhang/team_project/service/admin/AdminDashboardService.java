package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.seller.SellerRevenueDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.repository.OrderRepository;
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

        if (lastMonthIncome.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
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
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        List<Order> completedOrders = orderRepository.findByOrderDateGreaterThanEqualAndOrderStatus(
                startOfMonth.atStartOfDay(),
                OrderStatus.DELIVERED);

        Map<Long, SellerRevenueDTO> sellerStats = new HashMap<>();

        completedOrders.forEach(order -> {
            Long sellerId = order.getUser().getId();
            sellerStats.computeIfAbsent(sellerId, k -> new SellerRevenueDTO(
                    sellerId,
                    order.getUser().getLastName(),
                    BigDecimal.ZERO,
                    0));

            SellerRevenueDTO stats = sellerStats.get(sellerId);
            stats.setTotalRevenue(stats.getTotalRevenue().add(BigDecimal.valueOf(order.getTotalAmount())));
            stats.setTotalOrders(stats.getTotalOrders() + 1);
        });

        return sellerStats.values().stream()
                .sorted(Comparator.comparing(SellerRevenueDTO::getTotalRevenue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, BigDecimal> getRevenueDistribution() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Order> monthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(23, 59, 59),
                OrderStatus.DELIVERED);

        Map<String, BigDecimal> distribution = new LinkedHashMap<>();

        // Phân bổ theo tuần
        Map<Integer, BigDecimal> weeklyRevenue = monthOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().get(WeekFields.ISO.weekOfWeekBasedYear()),
                        Collectors.mapping(order -> BigDecimal.valueOf(order.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        weeklyRevenue.forEach((week, amount) -> distribution.put("Week " + week, amount));

        return distribution;
    }

    @Override
    public Map<String, Object> getMonthlyRevenue() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> monthlyData = new ArrayList<>();

        // Lấy dữ liệu 12 tháng gần nhất
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
            monthData.put("revenue", revenue);
            monthData.put("profit", profit);
            monthData.put("orders", totalOrders);

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