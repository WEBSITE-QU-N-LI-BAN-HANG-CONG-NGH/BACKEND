package com.webanhang.team_project.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        UserDetails hangvu= User.builder()
                .username("hangvu")
                .password("{noop}1")
                .roles("CUSTOMER")
                .build();

        UserDetails hantin= User.builder()
                .username("hantin")
                .password("{noop}1")
                .roles("CUSTOMER", "SELLER")
                .build();

        UserDetails tumay= User.builder()
                .username("tumay")
                .password("{noop}1")
                .roles("CUSTOMER", "SELLER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(tumay, hantin, hangvu);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(configurer ->
                    configurer
                            .requestMatchers("/").hasRole("CUSTOMER")
                            .requestMatchers("/seller/**").hasRole("SELLER")
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                )
                .formLogin(form ->
                            form
                                    .loginPage("/showMyLoggingPage")
                                    .loginProcessingUrl("/authenticateTheUser")
                                    .permitAll()
                        )
                .logout(LogoutConfigurer::permitAll
                )
                .exceptionHandling(configurer ->
                            configurer.accessDeniedPage("/access-denied")
                );

        return http.build();
    }
}
