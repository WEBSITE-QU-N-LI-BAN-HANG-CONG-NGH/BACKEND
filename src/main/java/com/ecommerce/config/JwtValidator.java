package com.ecommerce.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

//Mục đích: Filter để xác thực token JWT trong mỗi yêu cầu HTTP.


//extends OncePerRequestFilter: Đảm bảo filter chỉ được gọi một lần cho mỗi yêu cầu.
public class JwtValidator extends OncePerRequestFilter {

    private final SecretKey key;
    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);

    public JwtValidator(JwtConstant jwtConstant) {
        this.key = Keys.hmacShaKeyFor(jwtConstant.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = request.getHeader(JwtConstant.JWT_HEADER);
            logger.debug("Received JWT header: {}", jwt);

            if(jwt!=null && jwt.startsWith("Bearer ")) {
                try {
                    jwt = jwt.substring(7);
                    logger.debug("Processing JWT token: {}", jwt);

                    Claims claims = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(jwt)
                            .getPayload();

                    String email = String.valueOf(claims.get("email"));
                    logger.debug("Extracted email from JWT: {}", email);
                    
                    String authorities = String.valueOf(claims.get("authorities"));
                    logger.debug("Extracted authorities from JWT: {}", authorities);

                    List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Authentication set in SecurityContext for email: {}", email);
                } catch (Exception e) {
                    logger.error("JWT validation error: {}", e.getMessage(), e);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token: " + e.getMessage());
                    return;
                }
            } else {
                logger.debug("No JWT token found or invalid format in request");
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Unexpected error in JWT validation: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Server error during authentication: " + e.getMessage());
        }
    }

}
