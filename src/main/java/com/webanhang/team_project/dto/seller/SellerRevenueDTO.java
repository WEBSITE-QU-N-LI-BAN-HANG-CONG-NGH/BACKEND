package com.webanhang.team_project.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SellerRevenueDTO {
    private Long sellerId;
    private String sellerName;
    private BigDecimal totalRevenue;
    private int totalOrders;
}
