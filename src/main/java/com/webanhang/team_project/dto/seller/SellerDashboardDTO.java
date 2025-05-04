package com.webanhang.team_project.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardDTO {
    private BigDecimal totalRevenue;            // Tổng doanh thu
    private Integer totalOrders;                // Tổng số đơn hàng
    private Integer totalProducts;              // Tổng số sản phẩm
    private List<OrderStatsDTO> recentOrders;   // Đơn hàng gần đây
    private Map<String, BigDecimal> revenueByWeek;
    private Map<String, BigDecimal> revenueByMonth; // Doanh thu theo tháng
}
