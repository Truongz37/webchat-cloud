package com.truong.webchat_cloud.config;

import com.truong.webchat_cloud.entity.User;
import com.truong.webchat_cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm user trong Database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        // Chuyển đổi User của mình thành UserDetails của Spring Security
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // Mật khẩu này đã được mã hóa ở AuthController
                .roles(user.getRole().replace("ROLE_", "")) // Spring Security tự thêm tiền tố ROLE_
                .build();
    }
}