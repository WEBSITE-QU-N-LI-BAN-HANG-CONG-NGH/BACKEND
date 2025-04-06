package com.webanhang.team_project.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class CookieUtils {

    @Value("${app.useSecureCookie}")
    private boolean useSecureCookie;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    /**
     * Tạo và thêm cookie chứa refresh token vào HTTP response
     * Cookie được set với các thuộc tính bảo mật: HttpOnly, Secure, SameSite
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAge) {
        if (response == null) {
            throw new IllegalArgumentException("HttpServletResponse cannot be null");
        }
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (maxAge / 1000));
        refreshTokenCookie.setSecure(useSecureCookie);
        String sameSite = useSecureCookie ? "None" : "Lax";
        setResponseHeader(response, refreshTokenCookie, sameSite);
    }

    /**
     * Thiết lập header Set-Cookie với các thuộc tính bảo mật
     */
    private void setResponseHeader(HttpServletResponse response, Cookie refreshTokenCookie, String sameSite) {
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(refreshTokenCookie.getName()).append("=")
                .append(refreshTokenCookie.getValue())
                .append("; HttpOnly; Path=").append(refreshTokenCookie.getPath())
                .append("; Max-Age=").append(refreshTokenCookie.getMaxAge())
                .append(useSecureCookie ? "; Secure" : "")
                .append("; SameSite=").append(sameSite);
        response.setHeader("Set-Cookie", cookieHeader.toString());
    }

    /**
     * Lấy refresh token từ cookie
     */
    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                System.out.println("Names of the cookie found: " + cookie.getName());
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Xóa refresh token cookie
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Xóa cookie
        response.addCookie(cookie);
    }

    /**
     * In ra log tất cả cookies (dùng để debug)
     */
    public void logCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.info("Cookies from request:");
            for (Cookie cookie : cookies) {
                log.info("Cookie name: {}, value: {}", cookie.getName(), cookie.getValue());
            }
        } else {
            log.info("No cookies found in request");
        }
    }
}