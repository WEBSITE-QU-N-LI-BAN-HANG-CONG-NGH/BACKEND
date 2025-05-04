package com.webanhang.team_project.config;

import com.webanhang.team_project.security.CloudflareFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.filters.RateLimitFilter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;


// Cấu hình chung của ứng dụng, chứa các bean và thông tin cấu hình toàn cục.
@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
