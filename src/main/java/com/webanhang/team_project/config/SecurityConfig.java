package com.webanhang.team_project.config;

import com.webanhang.team_project.security.CloudflareFilter;
import com.webanhang.team_project.security.RateLimitFilter;
import com.webanhang.team_project.security.jwt.AuthTokenFilter;
import com.webanhang.team_project.security.jwt.JwtEntryPoint;
import com.webanhang.team_project.security.oauth2.OAuth2FailureHandler;
import com.webanhang.team_project.security.oauth2.OAuth2SuccessHandler;
import com.webanhang.team_project.security.userdetails.AppUserDetailsService;
import com.webanhang.team_project.utils.ErrorResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
//: Cấu hình bảo mật cho ứng dụng Spring Security, định nghĩa các quy tắc xác thực và phân quyền.
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    @Value("${api.prefix}")
    private String API;

    private final AppUserDetailsService userDetailsService;
    private final JwtEntryPoint authEntryPoint;
    private final AuthTokenFilter authTokenFilter;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final ErrorResponseUtils errorResponseUtils;
    private final PasswordEncoder passwordEncoder;
    private final CloudflareFilter cloudflareFilter;
    private final RateLimitFilter rateLimitFilter;



//  Cung cấp AuthenticationManager để xác thực người dùng.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    //Cấu hình DaoAuthenticationProvider sử dụng AppUserDetailsService và PasswordEncoder để xác thực.
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

//    Tùy chỉnh phản hồi khi người dùng bị từ chối truy cập (HTTP 403) bằng ErrorResponseUtils.
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                errorResponseUtils.sendAccessDeniedError(response,
                "You do not have permission to access this resource.");
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> securedUrls = List.of(API + "/cart/**", API + "/cartItems/**", API + "/orders/**");

        http
                .csrf(AbstractHttpConfigurer::disable)  //Tắt CSRF (csrf().disable()): Phù hợp với API không trạng thái (stateless) sử dụng JWT.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint)  //Thiết lập xử lý lỗi xác thực (authEntryPoint) và từ chối truy cập (accessDeniedHandler).
                        .accessDeniedHandler(accessDeniedHandler()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(API + "/auth/**").permitAll() // Cho phép tất cả các API xác thực
                        .requestMatchers(API + "/categories/**").permitAll() // Cho phép API danh mục công khai
                        .requestMatchers(API + "/products/**").permitAll() // Cho phép API sản phẩm công khai (xem)
                        .requestMatchers(API + "/contact/info").permitAll() // Cho phép API thông tin liên hệ
                        .requestMatchers(API + "/chatbot/**").permitAll() // Cho phép API chatbot
                        .requestMatchers(API + "/actuator/**").permitAll()
                        .requestMatchers(API + "/payment/vnpay-callback").permitAll() // Cho phép callback từ VNPay
                        .requestMatchers(API + "/admin/**").hasAuthority("ADMIN")
                        .requestMatchers(API + "/seller/**").hasAnyAuthority("SELLER")
                        .requestMatchers(API + "/customer/**").hasAnyAuthority("CUSTOMER")
                        .requestMatchers(securedUrls.toArray(String[]::new)).authenticated()
                        .requestMatchers("/oauth2/**").permitAll() // Vẫn cho phép OAuth2 flow
                        .anyRequest().authenticated()) // <--- THAY ĐỔI: Yêu cầu xác thực cho bất kỳ request nào khác
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(cloudflareFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, CloudflareFilter.class) // Đúng: rateLimitFilter sẽ được chèn vào ngay trước cloudflareFilter
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
