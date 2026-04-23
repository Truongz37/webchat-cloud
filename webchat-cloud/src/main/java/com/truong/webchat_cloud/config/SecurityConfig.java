package com.truong.webchat_cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt để dễ test với HTML thuần
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register.html", "/login.html", "/register", "/ws/**").permitAll() // Cho phép vào trang đăng ký
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Chỉ Admin mới vào được
                        .anyRequest().authenticated() // Còn lại phải đăng nhập
                )
                .formLogin(login -> login
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        // XÓA DÒNG defaultSuccessUrl CŨ VÀ THAY BẰNG ĐOẠN NÀY:
                        .successHandler((request, response, authentication) -> {
                            // Kiểm tra xem user đăng nhập có quyền ADMIN không
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                            if (isAdmin) {
                                response.sendRedirect("/admin.html");
                            } else {
                                response.sendRedirect("/chat.html");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout.permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Mã hóa mật khẩu bảo mật
    }
}
