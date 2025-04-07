package com.ecommerce.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;


/*
Mục đích: Tạo và xác thực token JWT.
 */
@Service
public class JwtProvider {
    private final SecretKey key;

    public JwtProvider(JwtConstant jwtConstant) {
        key= Keys.hmacShaKeyFor(jwtConstant.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /*
    Tạo token JWT:
    auth.getAuthorities(): Lấy danh sách quyền của người dùng.
    stream().map().collect(): Chuyển danh sách quyền thành chuỗi, phân tách bằng dấu phẩy.
    setIssuedAt: Thời gian phát hành token.
    setExpiration: Thời gian hết hạn (10 giờ = 1000ms * 60s * 60min * 10h).
    claim("email", auth.getName()): Thêm email vào claims.
    claim("authorities", authorities): Thêm quyền vào claims.
    signWith(key): Ký token bằng SecretKey.
    compact(): Tạo chuỗi token.
     */

    public String generateToken(Authentication auth) {
        String authorities=auth.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+1000*60*60*10))
                .claim("email", auth.getName())
                .claim("authorities", authorities)
                .signWith(key)
                .compact();
    }


    /*
    Lấy email từ token:
    Parse token để lấy Claims.
    Lấy giá trị email từ claims.
    Xử lý ngoại lệ nếu token không hợp lệ.
     */
    public String getEmailFromToken(String jwt) {
        try {
            Claims claims=Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return String.valueOf(claims.get("email"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token: "+ e.getMessage());
        }
    }
}
