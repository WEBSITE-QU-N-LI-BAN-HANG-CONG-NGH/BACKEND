package com.webanhang.team_project.controller.seller;

import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.seller.SellerDashboardDTO;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.seller.ISellerDashboardService;
import com.webanhang.team_project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/seller/dashboard")
public class SellerDashboardController {

    private final ISellerDashboardService sellerDashboardService;
    private final UserService userService;

    /**
     * Lấy dữ liệu tổng quan cho bảng điều khiển
     *
     * @param jwt Token xác thực người dùng
     * @return Dữ liệu tổng quan (doanh thu, đơn hàng, sản phẩm, ...)
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse> getDashboardOverview(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        SellerDashboardDTO dashboardData = sellerDashboardService.getDashboardData(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(dashboardData, "Lấy dữ liệu tổng quan thành công"));
    }

    /**
     * Lấy dữ liệu doanh thu theo tháng
     *
     * @param jwt Token xác thực người dùng
     * @return Dữ liệu doanh thu theo tháng
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse> getMonthlyRevenue(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        var revenueData = sellerDashboardService.getMonthlyRevenue(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(revenueData, "Lấy dữ liệu doanh thu thành công"));
    }

    /**
     * Lấy thống kê đơn hàng
     *
     * @param jwt Token xác thực người dùng
     * @return Thống kê đơn hàng theo trạng thái
     */
    @GetMapping("/orders/stats")
    public ResponseEntity<ApiResponse> getOrderStats(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        var orderStats = sellerDashboardService.getOrderStats(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(orderStats, "Lấy thống kê đơn hàng thành công"));
    }

    /**
     * Lấy thống kê sản phẩm
     *
     * @param jwt Token xác thực người dùng
     * @return Thống kê sản phẩm (còn hàng, hết hàng, sắp hết)
     */
    @GetMapping("/products/stats")
    public ResponseEntity<ApiResponse> getProductStats(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        var productStats = sellerDashboardService.getProductStats(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(productStats, "Lấy thống kê sản phẩm thành công"));
    }
}
