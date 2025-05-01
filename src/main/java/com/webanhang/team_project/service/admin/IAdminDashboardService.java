package com.webanhang.team_project.service.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.webanhang.team_project.dto.seller.SellerRevenueDTO;

public interface IAdminDashboardService {
    BigDecimal totalMonthInCome();
    BigDecimal compareToRecentMonthIncomeByPercent();
    BigDecimal compareToRecentMonthIncomeByVND();
    Map<String, Object> getMonthlyRevenue();
    List<SellerRevenueDTO> getTopSellers(int limit);
    Map<String, BigDecimal> getRevenueDistribution();

    Map<String, Object> getProductStatistics();

    Map<String, Object> getDashboardOverview();
}
