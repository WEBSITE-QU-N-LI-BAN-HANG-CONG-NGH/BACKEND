package com.webanhang.team_project.controller.common;

import com.webanhang.team_project.dto.auth.request.LoginRequest;
import com.webanhang.team_project.dto.auth.request.OtpVerificationRequest;
import com.webanhang.team_project.dto.auth.request.RegisterRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.user.UserDto;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.security.jwt.JwtUtils;
import com.webanhang.team_project.security.userdetails.AppUserDetails;
import com.webanhang.team_project.security.userdetails.AppUserDetailsService;
import com.webanhang.team_project.service.user.UserService;
import com.webanhang.team_project.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;
    private final AppUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${auth.token.refreshExpirationInMils}")
    private Long refreshTokenExpirationTime;

    /**
     * Xử lý đăng nhập và tạo cặp access token + refresh token
     * Access token trả về trong response body
     * Refresh token được lưu trong cookie
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> authenticateUser(@RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            String accessToken = jwtUtils.generateAccessToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(request.getEmail());
            cookieUtils.addRefreshTokenCookie(response, refreshToken, refreshTokenExpirationTime);

            AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId()).orElseThrow();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", accessToken);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("email", user.getEmail());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("role", user.getRole().getName().name());
            userMap.put("isActive", user.isActive());
            responseData.put("user", userMap);

            return ResponseEntity.ok(ApiResponse.success(responseData, "Success"));
        } catch (AuthenticationException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Error: Email hoặc mật khẩu không đúng!"));
        }
    }

    /**
     * Đăng ký tài khoản mới
     * Gửi OTP qua email để xác thực
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@RequestBody RegisterRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực đã được gửi tới email. Vui lòng kiểm tra và xác thực."));
    }

    /**
     * Xác thực OTP khi đăng ký
     */
    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody OtpVerificationRequest request) {
        try {
            boolean isVerified = userService.verifyOtp(request);
            if (isVerified) {
                return ResponseEntity.ok(ApiResponse.success(null, "Xác thực thành công! Tài khoản đã được kích hoạt."));
            }
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Mã OTP không hợp lệ hoặc đã hết hạn."));
        } catch (Exception e) {
            log.error("Lỗi xác thực OTP: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xác thực OTP: " + e.getMessage()));
        }
    }

    /**
     * Tạo access token mới từ refresh token
     * Kiểm tra tính hợp lệ của refresh token trong cookie
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse> refreshAccessToken(HttpServletRequest request) {
        try {
            String refreshToken = cookieUtils.getRefreshTokenFromCookies(request);
            if (refreshToken != null && jwtUtils.validateToken(refreshToken)) {
                String usernameFromToken = jwtUtils.getEmailFromToken(refreshToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(usernameFromToken);
                String newAccessToken = jwtUtils.generateAccessToken(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

                Map<String, String> token = new HashMap<>();
                token.put("accessToken", newAccessToken);
                return ResponseEntity.ok(ApiResponse.success(token, "Access token mới đã được tạo."));
            }
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Refresh token không hợp lệ hoặc đã hết hạn."));
        } catch (Exception e) {
            log.error("Lỗi refresh token: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi refresh token: " + e.getMessage()));
        }
    }

    /**
     * Đăng xuất - xóa refresh token cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletResponse response) {
        cookieUtils.deleteRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công!"));
    }

    /**
     * Kiểm tra trạng thái xác thực hiện tại
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse> checkAuthStatus(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("authenticated", true);
            userInfo.put("email", authentication.getName());
            userInfo.put("authorities", authentication.getAuthorities());
            return ResponseEntity.ok(ApiResponse.success(userInfo, "Đã xác thực."));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("authenticated", false), "Chưa xác thực."));
    }

    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse> getCurrentUser(Authentication authentication) {
        log.info("Authentication: {}", authentication);
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated"));
        }

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        User user = userService.getUserById(userDetails.getId());
        UserDto userDto = userService.convertUserToDto(user);

        return ResponseEntity.ok(ApiResponse.success(userDto, "Current user info retrieved successfully"));
    }
}