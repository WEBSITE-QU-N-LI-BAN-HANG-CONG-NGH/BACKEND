package com.webanhang.team_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitFilter extends OncePerRequestFilter {

    // Map lưu trữ số lượng request theo IP
    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();
    // Giới hạn request trong khoảng thời gian
    private final int rateLimit = 50; // Số request tối đa
    private final long timeWindow = 60000; // Khoảng thời gian (1 phút)

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy địa chỉ IP của client
        String clientIp = getClientIp(request);

        // Kiểm tra và cập nhật counter
        if (isRateLimited(clientIp)) {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            return;
        }

        // Tiếp tục chuỗi filter nếu không bị giới hạn
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Lấy IP đầu tiên trong chuỗi X-Forwarded-For
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();

        // Lấy hoặc tạo counter cho IP
        RequestCounter counter = requestCounters.computeIfAbsent(clientIp,
                ip -> new RequestCounter(now));

        // Reset counter nếu đã hết thời gian
        if (now - counter.getStartTime() > timeWindow) {
            counter.reset(now);
        }

        // Tăng số lượng request
        int count = counter.incrementAndGet();

        // Kiểm tra giới hạn
        return count > rateLimit;
    }

    // Lớp nội bộ để theo dõi số lượng request
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private long startTime;

        public RequestCounter(long startTime) {
            this.startTime = startTime;
        }

        public int incrementAndGet() {
            return count.incrementAndGet();
        }

        public long getStartTime() {
            return startTime;
        }

        public void reset(long newStartTime) {
            count.set(0);
            startTime = newStartTime;
        }
    }
}