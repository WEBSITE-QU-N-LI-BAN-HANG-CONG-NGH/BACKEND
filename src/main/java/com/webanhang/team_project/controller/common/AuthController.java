package com.webanhang.team_project.controller.common;

import com.webanhang.team_project.dto.auth.ForgotPasswordRequest;
import com.webanhang.team_project.dto.auth.LoginRequest;
import com.webanhang.team_project.dto.auth.OtpVerificationRequest;
import com.webanhang.team_project.dto.auth.RegisterRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.security.jwt.JwtUtils;
import com.webanhang.team_project.security.otp.OtpService;
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
@RequiredArgsConstructor   //Annotation của Lombok để tự động tạo constructor với các tham số là các trường final
@Slf4j
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;
    private final AppUserDetailsService userDetailsService;  // Service để tải thông tin người dùng
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final OtpService otpService;

    @Value("${auth.token.refreshExpirationInMils}")
    private Long refreshTokenExpirationTime;

    /**
     * Xử lý đăng nhập và tạo cặp access token + refresh token
     *
     * @param request Yêu cầu đăng nhập chứa email và mật khẩu
     * @param response HTTP response để lưu refresh token vào cookie
     * @return Thông tin đăng nhập thành công bao gồm access token và thông tin người dùng
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
            responseData.put("refreshToken", refreshToken);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("email", user.getEmail());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("role", user.getRole().getName().name());
            userMap.put("isActive", user.isActive());
            responseData.put("user", userMap);

            return ResponseEntity.ok(ApiResponse.success(responseData, "Đăng nhập thành công"));
        } catch (AuthenticationException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Email hoặc mật khẩu không đúng!"));
        }
    }

    /**
     * Đăng ký tài khoản mới và gửi OTP qua email để xác thực
     *
     * @param request Yêu cầu đăng ký chứa thông tin người dùng
     * @return Thông báo OTP đã được gửi
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@RequestBody RegisterRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực đã được gửi tới email. Vui lòng kiểm tra và xác thực."));
    }

    /**
     * Xác thực OTP khi đăng ký
     *
     * @param request Yêu cầu xác thực OTP chứa email và mã OTP
     * @return Thông báo xác thực thành công hoặc thất bại
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
     *
     * @param request HTTP request chứa refresh token trong cookie
     * @return Access token mới nếu refresh token hợp lệ
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
     *
     * @param response HTTP response để xóa cookie
     * @return Thông báo đăng xuất thành công
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletResponse response) {
        cookieUtils.deleteRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công!"));
    }

    /**
     * Kiểm tra trạng thái xác thực hiện tại
     *
     * @param authentication Đối tượng Authentication từ SecurityContext
     * @return Thông tin trạng thái xác thực
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
    /**
     * Lấy thông tin người dùng hiện tại
     *
     * @param authentication Đối tượng Authentication từ SecurityContext
     * @return Thông tin người dùng hiện tại
     */
    @GetMapping("/current-user-info")
    public ResponseEntity<ApiResponse> getCurrentUserInfo(Authentication authentication) {
        log.info("Authentication: {}", authentication);
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated"));
        }

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        User user = userService.getUserById(userDetails.getId());
        UserDTO userDto = userService.convertUserToDto(user);

        return ResponseEntity.ok(ApiResponse.success(userDto, "Current user info retrieved successfully"));
    }




    /**
     * Gửi lại OTP nếu người dùng không nhận được
     *
     * @param request Map chứa email cần gửi lại OTP
     * @return Thông báo OTP đã được gửi lại
     */
    @PostMapping("/register/resend-otp")
    public ResponseEntity<ApiResponse> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Email không được để trống."));
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Email chưa được đăng ký."));
        }

        if (user.isActive()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Tài khoản đã được kích hoạt."));
        }

        try {
            String otp = otpService.generateOtp(email);
            otpService.sendOtpEmail(email, otp);
            return ResponseEntity.ok(ApiResponse.success(null, "Mã OTP mới đã được gửi tới email."));
        } catch (Exception e) {
            log.error("Lỗi khi gửi lại OTP: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi gửi OTP: " + e.getMessage()));
        }
    }

    /**
     * Xử lý quên mật khẩu với xác thực OTP
     *
     * @param forgotPasswordRequest Yêu cầu đặt lại mật khẩu chứa email, OTP và mật khẩu mới
     * @return Thông báo thay đổi mật khẩu thành công
     */
    @PostMapping("/register/forgot-password")
    public ResponseEntity<ApiResponse> forgotPass(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        try {
            OtpVerificationRequest tmp =new OtpVerificationRequest();
            tmp.setEmail(forgotPasswordRequest.getEmail());
            tmp.setOtp(forgotPasswordRequest.getOtp());

            boolean isVerified = userService.verifyOtp(tmp);

            if (isVerified) {
                userService.forgotPassword(forgotPasswordRequest.getEmail(), forgotPasswordRequest.getNewPassword());
                return ResponseEntity.ok(ApiResponse.success(null, "Mật khẩu đã được thay đổi thành công!"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Mã OTP không hợp lệ hoặc đã hết hạn."));
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực OTP: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xác thực OTP: " + e.getMessage()));
        }
    }
}