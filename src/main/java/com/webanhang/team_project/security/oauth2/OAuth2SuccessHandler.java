package com.webanhang.team_project.security.oauth2;

// ... (Các import khác giữ nguyên)
import com.webanhang.team_project.model.User; // Cần thiết nếu AppUserDetails không có getter cho email
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.security.jwt.JwtUtils;
import com.webanhang.team_project.security.userdetails.AppUserDetails; // ** Thêm import này **
import com.webanhang.team_project.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
// import org.springframework.security.oauth2.core.user.OAuth2User; // Không cần trực tiếp nữa
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder; // ** Thêm import này cho redirect lỗi **

import java.io.IOException;
import java.net.URLEncoder; // ** Thêm import này cho redirect lỗi **
import java.nio.charset.StandardCharsets; // ** Thêm import này cho redirect lỗi **
// import java.util.Map; // Không cần trực tiếp nữa

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;
    // private final UserRepository userRepository; // Không cần trực tiếp nữa nếu dùng AppUserDetails

    @Value("${app.oauth2.redirectUri}")
    private String defaultRedirectUri;
    @Value("${app.oauth2.failureRedirectUri}") // ** Thêm dòng này để lấy failure URI **
    private String defaultFailureRedirectUri;
    @Value("${auth.token.refreshExpirationInMils}")
    private Long refreshTokenExpirationTime;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // ** Ép kiểu principal thành AppUserDetails **
        if (!(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            log.error("Principal is not an instance of AppUserDetails. Actual type: {}",
                    authentication.getPrincipal().getClass().getName());
            sendErrorRedirect(request, response, "invalid_principal_type", "Internal server error during login.");
            return;
        }

        // ** Lấy email trực tiếp từ AppUserDetails **
        String email = userDetails.getEmail(); // Hoặc userDetails.getUsername() tùy theo cách bạn cài đặt AppUserDetails

        if (email == null || email.isEmpty()) {
            // Trường hợp này không nên xảy ra nếu OAuth2UserService hoạt động đúng
            log.error("Email is null or empty in AppUserDetails for principal: {}", userDetails.getUsername());
            sendErrorRedirect(request, response, "email_extraction_failed", "Could not determine user email after login.");
            return;
        }

        // // Không cần tìm user trong DB nữa vì đã có trong AppUserDetails
        // User user = userRepository.findByEmail(email);
        // if (user == null) {
        //     log.error("User not found for email: {}", email);
        //     getRedirectStrategy().sendRedirect(request, response, "/login?error=user_not_found");
        //     return;
        // }

        // Tạo authentication mới với thông tin user từ AppUserDetails (đã lấy từ DB trong service)
        // Thực ra, authentication ban đầu đã chứa AppUserDetails rồi, có thể dùng luôn nó
        // Authentication userAuthentication = new UsernamePasswordAuthenticationToken(
        //         userDetails, null, userDetails.getAuthorities());

        // Tạo JWT token dùng authentication hiện tại (đã chứa AppUserDetails)
        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(email); // Dùng email lấy được

        // Lưu refresh token vào cookie
        cookieUtils.addRefreshTokenCookie(response, refreshToken, refreshTokenExpirationTime);

        // Redirect về frontend với access token trong query param
        String redirectUrl = buildRedirectUrl(accessToken);
        log.info("OAuth2 login successful for user {}, redirecting to: {}", email, redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // // Phương thức getEmailFromOAuth2User không còn cần thiết nữa
    // private String getEmailFromOAuth2User(OAuth2User oauth2User, Authentication authentication) { ... }

    private String buildRedirectUrl(String accessToken) {
        // Đảm bảo defaultRedirectUri không bị null
        String baseUri = (defaultRedirectUri != null) ? defaultRedirectUri : "/";
        return UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("token", accessToken)
                .build().toUriString();
    }

    // ** Phương thức trợ giúp để redirect lỗi về frontend (tương tự OAuth2FailureHandler) **
    private void sendErrorRedirect(HttpServletRequest request, HttpServletResponse response, String errorCode, String defaultMessage) throws IOException {
        String errorMessage = defaultMessage + " (Code: " + errorCode + ")";
        log.error("OAuth2 Success Handler Error - Redirecting with error: {}", errorMessage);

        String encodedErrorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        String redirectUrl = UriComponentsBuilder.fromUriString(defaultFailureRedirectUri) // Sử dụng failure URI đã cấu hình
                .queryParam("error", encodedErrorMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}