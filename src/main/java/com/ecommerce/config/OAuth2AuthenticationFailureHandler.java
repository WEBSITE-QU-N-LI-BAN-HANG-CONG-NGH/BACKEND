package com.ecommerce.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        logger.error("OAuth2 Authentication failure: {}", exception.getMessage(), exception);
        
        // Log thêm thông tin request để debug
        logger.debug("Request URI: {}", request.getRequestURI());
        logger.debug("Request parameters: {}", request.getParameterMap());
        
        // Mã hóa thông báo lỗi để tránh các vấn đề về URL encoding
        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8.toString());
        
        // Chuyển hướng về trang đăng nhập với thông báo lỗi
        String redirectUrl = "http://localhost:5173/login?error=" + errorMessage;
        logger.debug("Redirecting to: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
} 