package com.webanhang.team_project.service.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.webanhang.team_project.dto.dashboard.SellerRevenueDTO;

public interface IAdminDashboardService {
    // Phần 1: Tổng quan doanh thu
    BigDecimal totalMonthInCome();

    BigDecimal compareToRecentMonthIncomeByPercent();

    BigDecimal compareToRecentMonthIncomeByVND();

    // Phần 2: Xếp hạng người bán
    List<SellerRevenueDTO> getTopSellers(int limit);

    // Phần 3: Phân bổ doanh thu
    Map<String, BigDecimal> getRevenueDistribution();

}
