package com.webanhang.team_project.controller.admin;


import com.webanhang.team_project.dto.role.ChangeRoleRequest;
import com.webanhang.team_project.dto.user.UserDto;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.user.request.UpdateUserStatusRequest;
import com.webanhang.team_project.service.admin.ManageUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/users")
public class ManageUserController {

    private final ManageUserService adminUserService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {

        Page<UserDto> users = adminUserService.getAllUsers(page, size, search, role);
        return ResponseEntity.ok(ApiResponse.success(users, "Get all users success"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserDetails(@PathVariable int userId) {
        UserDto user = adminUserService.getUserDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "Get user details success"));
    }

    @PutMapping("/{userId}/change-role")
    public ResponseEntity<ApiResponse> changeUserRole(
            @PathVariable int userId,
            @RequestBody ChangeRoleRequest request) {

        UserDto updatedUser = adminUserService.changeUserRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Change user role success"));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable int userId,
            @RequestBody UpdateUserStatusRequest request) {

        UserDto updatedUser = adminUserService.updateUserStatus(userId, request.isActive());
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Update user status success"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable int userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete user success"));
    }
}
