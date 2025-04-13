package com.webanhang.team_project.controller.seller;

import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý các endpoint chung cho người bán
 * Bao gồm các chức năng xác thực vai trò, kiểm tra quyền,...
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/seller")
public class SellerManager {

    private final UserService userService;

    /**
     * Kiểm tra vai trò người bán
     */
    @GetMapping("/verify-role")
    public ResponseEntity<?> verifySellerRole(@RequestHeader("Authorization") String jwt) {
        try {
            User user = userService.findUserByJwt(jwt);
            boolean isSeller = user.getRole() != null && user.getRole().getName() == UserRole.SELLER;

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", true,
                            "isSeller", isSeller,
                            "message", isSeller ? "Người dùng có vai trò SELLER" : "Người dùng không có vai trò SELLER"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    /**
     * Kiểm tra trạng thái người bán
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSellerStatus(@RequestHeader("Authorization") String jwt) {
        try {
            User user = userService.findUserByJwt(jwt);

            // Kiểm tra vai trò
            if (user.getRole() == null || user.getRole().getName() != UserRole.SELLER) {
                return ResponseEntity.badRequest().body(
                        java.util.Map.of(
                                "success", false,
                                "message", "Người dùng không có vai trò SELLER"
                        )
                );
            }

            // Trả về trạng thái
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", true,
                            "isActive", user.isActive(),
                            "status", user.isActive() ? "ACTIVE" : "INACTIVE",
                            "message", user.isActive()
                                    ? "Tài khoản người bán đang hoạt động"
                                    : "Tài khoản người bán chưa được kích hoạt"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }
}
