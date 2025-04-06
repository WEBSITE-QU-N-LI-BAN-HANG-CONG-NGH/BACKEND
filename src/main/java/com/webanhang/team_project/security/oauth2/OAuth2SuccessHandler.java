package com.webanhang.team_project.security.oauth2;

import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.security.jwt.JwtUtils;
import com.webanhang.team_project.security.userdetails.AppUserDetails;
import com.webanhang.team_project.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirectUri}")
    private String defaultRedirectUri;
    @Value("${auth.token.refreshExpirationInMils}")
    private Long refreshTokenExpirationTime;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Lấy email từ OAuth2User
        String email = getEmailFromOAuth2User(oauth2User, authentication);

        if (email == null) {
            log.error("Cannot extract email from OAuth2 user");
            getRedirectStrategy().sendRedirect(request, response, "/login?error=email_not_found");
            return;
        }

        // Tìm User trong DB theo email
        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.error("User not found for email: {}", email);
            getRedirectStrategy().sendRedirect(request, response, "/login?error=user_not_found");
            return;
        }

        // Tạo authentication mới với thông tin user từ database
        AppUserDetails userDetails = AppUserDetails.buildUserDetails(user);
        Authentication userAuthentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // Tạo JWT token
        String accessToken = jwtUtils.generateAccessToken(userAuthentication);
        String refreshToken = jwtUtils.generateRefreshToken(email);

        // Lưu refresh token vào cookie
        cookieUtils.addRefreshTokenCookie(response, refreshToken, refreshTokenExpirationTime);

        // Redirect về frontend với access token trong query param
        String redirectUrl = buildRedirectUrl(accessToken);
        log.info("OAuth2 login successful, redirecting to: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String getEmailFromOAuth2User(OAuth2User oauth2User, Authentication authentication) {
        // Google luôn có email trong attributes
        if (oauth2User.getAttribute("email") != null) {
            return oauth2User.getAttribute("email");
        }

        // Các trường hợp khác, cần kiểm tra chi tiết hơn
        Map<String, Object> attributes = oauth2User.getAttributes();

        // GitHub và các provider khác
        if (attributes.containsKey("email")) {
            return (String) attributes.get("email");
        }

        log.warn("Email not found in OAuth2User attributes: {}", attributes);
        return null;
    }

    private String buildRedirectUrl(String accessToken) {
        return defaultRedirectUri + "?token=" + accessToken;
    }
}
