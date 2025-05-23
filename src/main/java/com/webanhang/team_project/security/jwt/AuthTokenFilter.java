package com.webanhang.team_project.security.jwt;

import com.webanhang.team_project.security.userdetails.AppUserDetailsService;

import com.webanhang.team_project.utils.ErrorResponseUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter xử lý xác thực JWT token cho mỗi request
 * Kiểm tra và set thông tin authentication vào SecurityContext
 *
 * Mục đích: Bộ lọc (filter) xử lý xác thực JWT token cho mỗi yêu cầu HTTP,
 * kiểm tra token hợp lệ và thiết lập thông tin xác thực vào SecurityContext của Spring Security.
 */

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {  /**
     * OncePerRequestFilter là một lớp trừu tượng trong Spring Security,
     * được sử dụng để tạo ra các bộ lọc (filter) mà chỉ chạy một lần cho mỗi yêu cầu HTTP.
     * Điều này giúp đảm bảo rằng bộ lọc của bạn sẽ không chạy nhiều lần cho cùng một yêu cầu,
     * điều này có thể xảy ra nếu bạn sử dụng các bộ lọc khác nhau trong chuỗi bộ lọc của Spring Security.
     */

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);
    private final JwtUtils jwtUtils;
    private final AppUserDetailsService userDetailsService;
    private final ErrorResponseUtils errorResponseUtils;

    @Override
    //Xử lý logic xác thực JWT.
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request); ///để trích xuất token từ header Authorization (định dạng Bearer <token>).
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {  /// Nếu token tồn tại và hợp lệ (kiểm tra bằng jwtUtils.validateToken(jwt)
                String username = jwtUtils.getEmailFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()); ///Tạo đối tượng UsernamePasswordAuthenticationToken chứa thông tin người dùng và quyền (authorities).
                SecurityContextHolder.getContext().setAuthentication(auth);  ///Lưu thông tin xác thực vào SecurityContextHolder để Spring Security sử dụng.
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage());
            sendErrorResponse(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Gửi response lỗi khi token không hợp lệ
     */
    private void sendErrorResponse(HttpServletResponse response) throws IOException {
        errorResponseUtils.sendAuthenticationError(response,
                "Token truy cập không hợp lệ, vui lòng đăng nhập và thử lại!");
    }

    /**
     * Trích xuất JWT token từ Authorization header
     * Format header: "Bearer <token>"
     */
    public String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
