package com.ecommerce.config;

import com.ecommerce.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired
    private JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        logger.debug("OAuth2 Authentication success handler được kích hoạt");
        logger.debug("Authentication details: {}", authentication);
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        logger.debug("OAuth2User attributes: {}", oAuth2User.getAttributes());
        
        // Lấy email từ OAuth2User
        String email = oAuth2User.getAttribute("email");
        logger.debug("Email từ OAuth2User: {}", email);
        
        // Tạo JWT token
        String token = jwtService.generateToken(email);
        logger.debug("JWT token đã được tạo cho email: {}", email);
        
        // Trả về token trong response header
        response.setHeader("Authorization", "Bearer " + token);
        
        // Chuyển hướng về trang chủ frontend với token
        String redirectUrl = UriComponentsBuilder
                .fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", token)
                .build()
                .toUriString();
        logger.debug("Chuyển hướng đến: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}