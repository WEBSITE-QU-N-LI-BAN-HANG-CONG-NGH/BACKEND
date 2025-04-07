package com.ecommerce.service;

import com.ecommerce.config.JwtConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    @Autowired
    public JwtService(JwtConstant jwtConstant) {
        key = Keys.hmacShaKeyFor(jwtConstant.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 giờ
                .claim("email", email)
                .claim("authorities", "ROLE_CUSTOMER") // Mặc định cho OAuth2
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return String.valueOf(claims.get("email"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token: " + e.getMessage());
        }
    }
}