package com.webanhang.team_project.service.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IAdminReportService {
    List<Map<String, Object>> generateProductReports(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getProductStatistics();
}
