package com.webanhang.team_project.controller.admin;


import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.role.ChangeRoleRequest;
import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.dto.user.UpdateUserStatusRequest;
import com.webanhang.team_project.service.admin.IAdminManageUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/users")
public class AdminManageUserController {

    private final IAdminManageUserService adminUserService;

    /**
     * Lấy danh sách tất cả người dùng với phân trang và lọc
     *
     * @param page   Số trang, mặc định là 0
     * @param size   Số lượng bản ghi trên một trang, mặc định là 10
     * @param search Từ khóa tìm kiếm theo email hoặc tên
     * @param role   Lọc theo vai trò (ADMIN, SELLER, CUSTOMER)
     * @return Danh sách người dùng đã được phân trang và lọc
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {

        Page<UserDTO> users = adminUserService.getAllUsers(page, size, search, role);
        return ResponseEntity.ok(ApiResponse.success(users, "Get all users success"));
    }

    /**
     * Lấy thông tin chi tiết của một người dùng
     *
     * @param  userId ID của người dùng cần xem
     * @return Thông tin chi tiết của người dùng
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserDetails(@PathVariable Long userId) {
        UserDTO user = adminUserService.getUserDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "Get user details success"));
    }

    /**
     * Thay đổi vai trò của người dùng
     *
     * @param  userId ID của người dùng cần thay đổi
     * @param  request Yêu cầu thay đổi vai trò
     * @return Thông tin người dùng sau khi cập nhật
     */
    @PutMapping("/{userId}/change-role")
    public ResponseEntity<ApiResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestBody ChangeRoleRequest request) {

        UserDTO updatedUser = adminUserService.changeUserRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Change user role success"));
    }

    /**
     * Cập nhật trạng thái hoạt động của người dùng
     *
     * @param  userId ID của người dùng cần cập nhật
     * @param  request Yêu cầu cập nhật (active = true/false)
     * @return Thông tin người dùng sau khi cập nhật
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UpdateUserStatusRequest request) {

        UserDTO updatedUser = adminUserService.updateUserStatus(userId, request.isActive());
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Update user status success"));
    }

    /**
     * Xóa người dùng khỏi hệ thống
     *
     * @param  userId ID của người dùng cần xóa
     * @return Thông báo kết quả
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete user success"));
    }

    /**
     * Lấy thống kê về khách hàng
     *
     * @return Dữ liệu thống kê về khách hàng
     */
    @GetMapping("/customers/stats")
    public ResponseEntity<ApiResponse> getCustomerStats() {
        Map<String, Object> stats = adminUserService.getCustomerStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Get customer statistics success"));
    }
}
