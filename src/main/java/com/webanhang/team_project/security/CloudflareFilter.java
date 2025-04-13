package com.webanhang.team_project.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloudflareFilter extends OncePerRequestFilter {
    @Value("${cloudflare.trusted-proxies}")
    private String trustedProxiesConfig;

    private List<IPRange> trustedProxies = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Parse trusted proxies from config
        String[] ranges = trustedProxiesConfig.split(",");
        for (String range : ranges) {
            trustedProxies.add(new IPRange(range));
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String remoteAddr = request.getRemoteAddr();

        boolean fromCloudflare = false;
        for (IPRange range : trustedProxies) {
            if (range.contains(remoteAddr)) {
                fromCloudflare = true;
                break;
            }
        }

        if (!fromCloudflare) {
            response.setStatus(403);
            response.getWriter().write("Access denied");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class IPRange {
        // Simple implementation to check if an IP is in a CIDR range
        // ...
    }
}
