package com.truong.webchat_cloud.controller;

import com.truong.webchat_cloud.entity.User;
import com.truong.webchat_cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // API xử lý đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại!");
        }
        User user = new User();
        user.setUsername(username);
        // Bắt buộc phải mã hóa mật khẩu trước khi lưu vào DB
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        return ResponseEntity.ok("Đăng ký thành công!");
    }

    // API để file HTML (giao diện) lấy tên người đang đăng nhập
    @GetMapping("/api/auth/me")
    public ResponseEntity<String> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(auth.getName());
        }
        return ResponseEntity.status(401).body("Chưa đăng nhập");
    }
}