package com.webanhang.team_project.security.ratelimit;

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

    private final int MAX_REQUESTS = 100;
    private final long TIME_WINDOW = 60 * 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);

        if (isRateLimited(request, clientIp)) {
            response.setStatus(429); // 429
            response.getWriter().write("Quá nhiều yêu cầu. Vui lòng thử lại sau.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        Object realIp = request.getAttribute("REAL_CLIENT_IP");
        if (realIp != null) {
            return realIp.toString();
        }

        return request.getRemoteAddr();
    }

    private boolean isRateLimited(HttpServletRequest request, String clientIp) {
        long now = System.currentTimeMillis();
        String requestPath = request.getRequestURI();
        String key = clientIp + ":" + requestPath;

        RequestCount count = requestCounts.computeIfAbsent(key,
                k -> new RequestCount(now, 0));

        if (now - count.timestamp > TIME_WINDOW) {
            count.timestamp = now;
            count.count = 1;
            return false;
        }

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