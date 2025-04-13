package com.webanhang.team_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CloudflareFilter extends OncePerRequestFilter {

    // Danh sách các dải IP của Cloudflare
    // Bạn cần cập nhật danh sách này từ: https://www.cloudflare.com/ips/
    private final List<String> cloudflareIpRanges = new ArrayList<>();

    public CloudflareFilter() {
        // IPv4 ranges
        cloudflareIpRanges.add("173.245.48.0/20");
        cloudflareIpRanges.add("103.21.244.0/22");
        // Thêm các dải IP khác của Cloudflare ở đây
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy IP của request
        String remoteAddr = request.getRemoteAddr();

        // Kiểm tra xem request có đến từ Cloudflare không
        if (isCloudflareIp(remoteAddr)) {
            // Nếu từ Cloudflare, sử dụng CF-Connecting-IP header
            String realClientIp = request.getHeader("CF-Connecting-IP");
            if (realClientIp != null && !realClientIp.isEmpty()) {
                // Đặt IP thật vào request attribute để sử dụng sau này
                request.setAttribute("REAL_CLIENT_IP", realClientIp);
            }
        }

        // Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }

    private boolean isCloudflareIp(String ipAddress) {
        // Kiểm tra IP có thuộc dải Cloudflare không
        for (String range : cloudflareIpRanges) {
            if (isIpInRange(ipAddress, range)) {
                return true;
            }
        }
        return false;
    }

    // Phương thức đơn giản để kiểm tra xem một IP có thuộc một dải CIDR không
    private boolean isIpInRange(String ip, String cidr) {
        // Triển khai logic kiểm tra IP trong dải CIDR
        // Đây là một hiện thực đơn giản, bạn có thể sử dụng thư viện như apache.commons.net
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefix;

            if (parts.length > 1) {
                prefix = Integer.parseInt(parts[1]);
            } else {
                if (network.contains(".")) {
                    prefix = 32; // Nếu IPv4
                } else {
                    prefix = 128; // Nếu IPv6
                }
            }

            // Logic kiểm tra IP - phiên bản đơn giản
            // Đối với triển khai thực tế, bạn nên sử dụng thư viện

            // Ví dụ đơn giản cho IPv4:
            if (network.equals(ip)) {
                return true;
            }

            // Đây là phương pháp rất đơn giản, không chính xác cho tất cả trường hợp
            if (ip.startsWith(network.substring(0, network.lastIndexOf('.')))) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}