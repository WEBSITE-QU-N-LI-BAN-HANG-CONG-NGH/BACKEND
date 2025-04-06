package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.dashboard.SellerRevenueDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse> getDashboardOverview() {
        Map<String, Object> response = new HashMap<>();
        response.put("revenue", getRevenueOverview());
        response.put("topSellers", getTopSellers());
        response.put("distribution", getRevenueDistribution());
        return ResponseEntity.ok(ApiResponse.success(response, "Get dashboard overview success"));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse> getRevenueOverview() {
        Map<String, Object> revenue = new HashMap<>();
        revenue.put("currentMonthIncome", adminDashboardService.totalMonthInCome());
        revenue.put("comparePercent", adminDashboardService.compareToRecentMonthIncomeByPercent());
        revenue.put("compareDifference", adminDashboardService.compareToRecentMonthIncomeByVND());
        return ResponseEntity.ok(ApiResponse.success(revenue, "Get revenue overview success"));
    }

    @GetMapping("/top-sellers")
    public ResponseEntity<ApiResponse> getTopSellers() {
        List<SellerRevenueDTO> topSellers = adminDashboardService.getTopSellers(5);
        return ResponseEntity.ok(ApiResponse.success(topSellers, "Get top sellers success"));
    }

    @GetMapping("/revenue-distribution")
    public ResponseEntity<ApiResponse> getRevenueDistribution() {
        Map<String, BigDecimal> distribution = adminDashboardService.getRevenueDistribution();
        return ResponseEntity.ok(ApiResponse.success(distribution, "Get revenue distribution success"));
    }
}
