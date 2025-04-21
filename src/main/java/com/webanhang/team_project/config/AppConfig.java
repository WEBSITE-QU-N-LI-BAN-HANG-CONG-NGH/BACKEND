package com.webanhang.team_project.config;

import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;


// Cấu hình chung của ứng dụng, chứa các bean và thông tin cấu hình toàn cục.
@Configuration
public class AppConfig {

    /*
    Mục đích: Tạo một bean ModelMapper, là một thư viện dùng để ánh xạ (mapping) giữa các đối tượng, ví dụ chuyển đổi từ DTO (Data Transfer Object) sang entity hoặc ngược lại.
    Cách dùng: Giúp đơn giản hóa việc ánh xạ bằng cách tự động khớp các trường có tên giống nhau, giảm mã lặp lại.
    Ví dụ: Nếu bạn có UserDTO và User entity, ModelMapper có thể tự động ánh xạ userDTO.getName() sang user.getName().
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }



    /*
    Mục đích: Tạo một bean PasswordEncoder, sử dụng BCrypt để mã hóa mật khẩu.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean // Đánh dấu phương thức này sẽ tạo ra một Bean được quản lý bởi Spring
    public RestTemplate restTemplate() {
        // Tạo và trả về một đối tượng RestTemplate mới
        return new RestTemplate();
    }
}
