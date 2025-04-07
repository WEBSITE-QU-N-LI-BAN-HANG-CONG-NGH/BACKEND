package com.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConstant {

    @Value("${jwt.secret}")
    private String secretKey;

//    Tên header chứa token JWT, đúng chuẩn cho Bearer token.
    public static final String JWT_HEADER="Authorization";

    public String getSecretKey() {
        return secretKey;
    }
}
