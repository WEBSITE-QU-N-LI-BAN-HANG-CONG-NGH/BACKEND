package com.webanhang.team_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RequestCount> requestCounts = new ConcurrentHashMap<>();

    // Số request tối đa trong 1 phút
    private final int MAX_REQUESTS = 50;
    private final long TIME_WINDOW = 60 * 1000; // 1 phút

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy địa chỉ IP của người dùng, ưu tiên từ CloudflareFilter
        String clientIp = getClientIp(request);

        // Kiểm tra giới hạn
        if (isRateLimited(clientIp)) {
            response.setStatus(429); // 429
            response.getWriter().write("Quá nhiều yêu cầu. Vui lòng thử lại sau.");
            return;
        }

        // Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // Lấy từ CloudflareFilter (nếu có)
        Object realIp = request.getAttribute("REAL_CLIENT_IP");
        if (realIp != null) {
            return realIp.toString();
        }

        // Nếu không có, lấy IP thông thường
        return request.getRemoteAddr();
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();

        // Lấy hoặc tạo counter
        RequestCount count = requestCounts.computeIfAbsent(clientIp,
                k -> new RequestCount(now, 0));

        // Kiểm tra xem có quá thời gian giới hạn không
        if (now - count.timestamp > TIME_WINDOW) {
            count.timestamp = now;
            count.count = 1;
            return false;
        }

        // Tăng counter và kiểm tra
        count.count++;
        return count.count > MAX_REQUESTS;
    }

    private static class RequestCount {
        long timestamp;
        int count;

        RequestCount(long timestamp, int count) {
            this.timestamp = timestamp;
            this.count = count;
        }
    }
}