package com.truong.webchat_cloud.config; // Đổi lại package cho đúng với project của bạn

import com.truong.webchat_cloud.entity.User;
import com.truong.webchat_cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem tài khoản admin đã tồn tại chưa
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Mật khẩu mặc định là admin123
            admin.setRole("ROLE_ADMIN");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setLocked(false);

            userRepository.save(admin);
            System.out.println("============== TẠO THÀNH CÔNG TÀI KHOẢN ADMIN ==============");
            System.out.println("Tên đăng nhập: admin");
            System.out.println("Mật khẩu: admin123");
            System.out.println("============================================================");
        }
    }
}