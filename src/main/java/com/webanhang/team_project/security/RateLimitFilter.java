package com.webanhang.team_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> blockUntil = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        // Kiểm tra nếu IP bị chặn
        if (blockUntil.containsKey(ip) && System.currentTimeMillis() < blockUntil.get(ip)) {
            response.setStatus(429);
            response.getWriter().write("Too many requests");
            return;
        }

        // Đếm request
        int count = requestCounts.getOrDefault(ip, 0) + 1;
        requestCounts.put(ip, count);

        // Rate limit: 100 requests per minute
        if (count > 100) {
            blockUntil.put(ip, System.currentTimeMillis() + 60000);
            response.setStatus(429);
            response.getWriter().write("Too many requests");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
