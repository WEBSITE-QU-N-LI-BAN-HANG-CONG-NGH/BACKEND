package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.dashboard.SellerRevenueDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService implements IAdminDashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public BigDecimal totalMonthInCome() {
        LocalDate now = LocalDate.now();
        List<Order> monthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                now.withDayOfMonth(1).atStartOfDay(),
                now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59),
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

        return currentMonthIncome.subtract(lastMonthIncome)
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

    private BigDecimal getLastMonthIncome() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        List<Order> lastMonthOrders = orderRepository.findByOrderDateBetweenAndOrderStatus(
                lastMonth.withDayOfMonth(1).atStartOfDay(),
                lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(23, 59, 59),
                OrderStatus.DELIVERED);

        return lastMonthOrders.stream()
                .map(Order::getTotalAmount)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}