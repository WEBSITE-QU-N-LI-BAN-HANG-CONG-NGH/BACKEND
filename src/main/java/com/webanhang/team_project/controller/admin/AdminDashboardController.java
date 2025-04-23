package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.seller.SellerRevenueDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.admin.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/dashboard")
public class AdminDashboardController {

    private final IAdminDashboardService adminDashboardService;

    /**
     * Lấy tổng quan bảng điều khiển
     *
     * @return Dữ liệu tổng quan bảng điều khiển bao gồm doanh thu, người bán hàng đầu và phân phối
     */
    @Transactional
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse> getDashboardOverview() {
        Map<String, Object> response = new HashMap<>();
        response.put("revenue", getRevenueOverview());
        response.put("topSellers", getTopSellers());
        response.put("distribution", getRevenueDistribution());
        return ResponseEntity.ok(ApiResponse.success(response, "Get dashboard overview success"));
    }

    /**
     * Lấy tổng quan doanh thu
     *
     * @return Thông tin tổng quan về doanh thu hiện tại, so sánh với tháng trước
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse> getRevenueOverview() {
        Map<String, Object> revenue = new HashMap<>();
        revenue.put("currentMonthIncome", adminDashboardService.totalMonthInCome());
        revenue.put("comparePercent", adminDashboardService.compareToRecentMonthIncomeByPercent());
        revenue.put("compareDifference", adminDashboardService.compareToRecentMonthIncomeByVND());
        return ResponseEntity.ok(ApiResponse.success(revenue, "Get revenue overview success"));
    }

    /**
     * Lấy doanh thu theo tháng
     *
     * @return Dữ liệu doanh thu theo từng tháng
     */
    @GetMapping("/revenue/monthly")
    public ResponseEntity<ApiResponse> getMonthlyRevenue() {
        Map<String, Object> monthlyData = adminDashboardService.getMonthlyRevenue();
        return ResponseEntity.ok(ApiResponse.success(monthlyData, "Get monthly revenue success"));
    }

    /**
     * Lấy danh sách người bán hàng đầu
     *
     * @return Danh sách người bán có doanh thu cao nhất
     */
    @GetMapping("/top-sellers")
    public ResponseEntity<ApiResponse> getTopSellers() {
        List<SellerRevenueDTO> topSellers = adminDashboardService.getTopSellers(5);
        return ResponseEntity.ok(ApiResponse.success(topSellers, "Get top sellers success"));
    }

    /**
     * Lấy phân phối doanh thu
     *
     * @return Thông tin phân phối doanh thu theo các phân loại
     */
    @GetMapping("/revenue-distribution")
    public ResponseEntity<ApiResponse> getRevenueDistribution() {
        Map<String, BigDecimal> distribution = adminDashboardService.getRevenueDistribution();
        return ResponseEntity.ok(ApiResponse.success(distribution, "Get revenue distribution success"));
    }
}
