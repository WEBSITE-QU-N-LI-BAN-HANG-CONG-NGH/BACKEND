package com.webanhang.team_project.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CloudflareFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy header CF-Connecting-IP từ Cloudflare
        String realClientIp = request.getHeader("CF-Connecting-IP");

        // Nếu có, lưu vào request attribute
        if (realClientIp != null && !realClientIp.isEmpty()) {
            request.setAttribute("REAL_CLIENT_IP", realClientIp);
        }

        // Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }
}