package com.webanhang.team_project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration: Đánh dấu đây là lớp cấu hình.
@Configuration
public class CorsConfig implements WebMvcConfigurer {
//    implements WebMvcConfigurer: Cho phép tùy chỉnh cấu hình Spring MVC, cụ thể là các thiết lập CORS.

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    @Value("${api.prefix}")
    private String API;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(API + "/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE","OPTIONS")
                .allowedHeaders("*")  //Cho phép tất cả các tiêu đề (header) trong yêu cầu.
                .exposedHeaders("CF-IPCountry", "CF-RAY", "CF-Connecting-IP") // chấp nhận header từ CloudFlare
                .allowCredentials(true) //Cho phép gửi cookie và thông tin xác thực (ví dụ: header Authorization).
                .maxAge(3600);
    }
}
